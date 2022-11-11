package org.tidepool.keycloak.extensions.resource;

import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

public abstract class AdminResource {

    @Context
    private HttpHeaders headers;

    @Context
    private KeycloakSession session;

    protected AdminPermissionEvaluator auth;

    protected void setup() {
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
        if (realm == null || !realm.equals(realmManager.getKeycloakAdminstrationRealm())) {
            throw new NotAuthorizedException("Unknown realm");
        }

        // Temporarily set the realm in the context to the admin realm to make sure we have a valid admin token
        session.getContext().setRealm(realm);

        var authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        authenticator.setRealm(realm);
        AuthenticationManager.AuthResult authResult = authenticator.authenticate();
        if (authResult == null) {
            throw new NotAuthorizedException("Bearer");
        }

        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null) {
            throw new NotFoundException("Could not find client for authorization");
        }

        AdminAuth adminAuth = new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client);
        this.auth = AdminPermissions.evaluator(session, realm, adminAuth);

        // Restore the original realm in the context
        session.getContext().setRealm(realmInContext);
    }
}
