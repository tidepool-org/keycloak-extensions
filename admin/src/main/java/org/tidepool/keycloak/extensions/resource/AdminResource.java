package org.tidepool.keycloak.extensions.resource;

import org.keycloak.Config;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.mappers.AdminApiAudienceMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.util.TokenUtil;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;

public abstract class AdminResource {

    final private KeycloakSession session;

    protected AdminPermissionEvaluator auth;
    private AdminAuth adminAuth;

    public AdminResource(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Permission evaluator scoped to the given realm. {@link #auth} is scoped to the admin (master)
     * realm; endpoints that mutate the realm in the request path should check permissions against that
     * target realm instead &mdash; the same way Keycloak's own admin endpoints evaluate a master-realm
     * token against the administered realm.
     */
    protected AdminPermissionEvaluator authFor(RealmModel realm) {
        return AdminPermissions.evaluator(session, realm, adminAuth);
    }

    protected void setup() {
        HttpHeaders headers = session.getContext().getRequestHeaders();
        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) throw new NotAuthorizedException("Bearer");
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        RealmModel realmInContext = session.getContext().getRealm();
        if (realm == null || !realm.equals(realmManager.getKeycloakAdministrationRealm())) {
            throw new NotAuthorizedException("Unknown realm");
        }

        // Temporarily set the realm in the context to the admin realm to make sure we have a valid admin token
        session.getContext().setRealm(realm);
        try {
            boolean audienceCheckEnabled = isAudienceCheckEnabled();

            var authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
            authenticator.setRealm(realm);
            if (audienceCheckEnabled) {
                // Same check Keycloak's own Admin REST API applies since 26.8.2 (CVE-2026-19688,
                // AdminRoot#authenticateRealmAdminRequest): the token must have been issued for the
                // Admin API, not merely by the admin realm.
                authenticator.setAudience(AdminApiAudienceMapper.getAdminApiAudience(session, realm));
            }
            AuthenticationManager.AuthResult authResult = authenticator.authenticate();
            if (authResult == null) {
                throw new NotAuthorizedException("Bearer");
            }
            if (audienceCheckEnabled
                    && !TokenUtil.hasScope(authResult.token().getScope(), Constants.KEYCLOAK_ADMIN_API_SCOPE)) {
                throw new NotAuthorizedException("Bearer");
            }

            ClientModel client = realm.getClientByClientId(token.getIssuedFor());
            if (client == null) {
                throw new NotFoundException("Could not find client for authorization");
            }

            AdminAuth adminAuth = new AdminAuth(realm, authResult.token(), authResult.user(), client);
            this.adminAuth = adminAuth;
            this.auth = AdminPermissions.evaluator(session, realm, adminAuth);
        } finally {
            // Restore the original realm in the context
            session.getContext().setRealm(realmInContext);
        }
    }

    /**
     * Mirrors the switch Keycloak uses for its own Admin REST API
     * ({@code --spi-admin--audience-check-enabled}, default {@code true}), so both admin surfaces are
     * either checked or not.
     */
    protected boolean isAudienceCheckEnabled() {
        return Config.scope("admin").getBoolean("audienceCheckEnabled", true);
    }
}
