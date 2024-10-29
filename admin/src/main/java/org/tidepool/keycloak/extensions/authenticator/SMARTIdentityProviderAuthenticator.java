package org.tidepool.keycloak.extensions.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.Authenticator;
import org.keycloak.events.Errors;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Optional;

public class SMARTIdentityProviderAuthenticator implements Authenticator {

    private static final Logger LOG = Logger.getLogger(SMARTIdentityProviderAuthenticator.class);

    protected static final String ACCEPTS_PROMPT_NONE = "acceptsPromptNoneForwardFromClient";

    public static final String CORRELATION_ID = "correlation_id";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String issuer = context.getUriInfo().getQueryParameters().getFirst(OIDCLoginProtocol.ISSUER);
        if (issuer == null || issuer.isBlank()) {
            LOG.warnf("No issuer %s query parameter provided", OIDCLoginProtocol.ISSUER);
            respondWithInvalidRequest(context, OIDCLoginProtocol.ISSUER + "query parameter is required");
            return;
        }

        String correlationId = context.getUriInfo().getQueryParameters().getFirst(CORRELATION_ID);
        if (correlationId == null || issuer.isBlank()) {
            LOG.warnf("No correlationId %s query parameter provided", CORRELATION_ID);
            respondWithInvalidRequest(context, CORRELATION_ID + "query parameter is required");
            return;
        }

        context.getAuthenticationSession().setClientNote(CORRELATION_ID, correlationId);

        LOG.infof("Redirecting with correlationId %s: %s set to %s", correlationId, OIDCLoginProtocol.ISSUER, issuer);
        redirect(context, issuer);
    }

    protected void respondWithInvalidRequest(AuthenticationFlowContext context, String errorMessage) {
        context.getEvent().error(Errors.IDENTITY_PROVIDER_ERROR);
        Response challenge = context.form().setError("Invalid request: " + errorMessage).createErrorPage(Response.Status.BAD_REQUEST);
        context.failure(AuthenticationFlowError.IDENTITY_PROVIDER_ERROR, challenge, "Invalid request", errorMessage);
    }

    protected void redirect(AuthenticationFlowContext context, String issuer) {
        Optional<IdentityProviderModel> idp = context.getRealm().getIdentityProvidersStream()
                .filter(IdentityProviderModel::isEnabled)
                .filter(identityProvider -> identityProvider.getConfig().getOrDefault("issuer", "").equals(issuer))
                .findFirst();
        if (idp.isPresent()) {
            String providerId = idp.get().getProviderId();

            String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(), context.getAuthenticationSession()).getOrGenerateCode();
            String clientId = context.getAuthenticationSession().getClient().getClientId();
            String tabId = context.getAuthenticationSession().getTabId();
            String clientData = AuthenticationProcessor.getClientData(context.getSession(), context.getAuthenticationSession());
            URI location = Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), providerId, context.getRealm().getName(), accessCode, clientId, tabId, clientData, null);
            Response response = Response.seeOther(location)
                    .build();

            // will forward the request to the IDP with prompt=none if the IDP accepts forwards with prompt=none.
            if ("none".equals(context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.PROMPT_PARAM)) &&
                    Boolean.parseBoolean(idp.get().getConfig().get(ACCEPTS_PROMPT_NONE))) {
                context.getAuthenticationSession().setAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN, "true");
            }

            LOG.debugf("Redirecting to %s", providerId);
            context.forceChallenge(response);
            return;
        }

        LOG.warnf("Smart issuer %s not found or not enabled for realm", issuer);
        context.attempted();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

}
