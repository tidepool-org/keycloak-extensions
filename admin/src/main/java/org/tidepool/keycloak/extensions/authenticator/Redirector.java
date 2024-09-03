package org.tidepool.keycloak.extensions.authenticator;

import de.sventorben.keycloak.authentication.hidpd.Users;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.keycloak.services.resources.IdentityBrokerService.getIdentityProviderFactory;

// This file is a copy of org.tidepool.keycloak.extensions.authenticator.Redirector with class and method access modifiers
// changed to public. This is required to allow HomeIdpDiscoveryLoginHintAuthenticator use this class
// and avoid maintaining our own fork.
public final class Redirector {

    private static final Logger LOG = Logger.getLogger(Redirector.class);

    private final AuthenticationFlowContext context;

    public Redirector(AuthenticationFlowContext context) {
        this.context = context;
    }

    public void redirectTo(IdentityProviderModel idp) {
        String providerAlias = idp.getAlias();
        RealmModel realm = context.getRealm();
        AuthenticationSessionModel authenticationSession = context.getAuthenticationSession();
        KeycloakSession keycloakSession = context.getSession();
        ClientSessionCode<AuthenticationSessionModel> clientSessionCode =
            new ClientSessionCode<>(keycloakSession, realm, authenticationSession);
        clientSessionCode.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        if (!idp.isEnabled()) {
            LOG.warnf("Identity Provider %s is disabled.", providerAlias);
            return;
        }
        if (idp.isLinkOnly()) {
            LOG.warnf("Identity Provider %s is not allowed to perform a login.", providerAlias);
            return;
        }

        LoginHint loginHint = new LoginHint(context, new Users(context.getSession()));
        loginHint.copyTo(clientSessionCode);

        IdentityProviderFactory<?> providerFactory = getIdentityProviderFactory(keycloakSession, idp);
        IdentityProvider<?> identityProvider = providerFactory.create(keycloakSession, idp);

        Response response = identityProvider.performLogin(createAuthenticationRequest(providerAlias, identityProvider, clientSessionCode));
        context.forceChallenge(response);
    }

    private AuthenticationRequest createAuthenticationRequest(String providerAlias, IdentityProvider<?> identityProvider, ClientSessionCode<AuthenticationSessionModel> clientSessionCode) {
        AuthenticationSessionModel authSession = null;
        IdentityBrokerState encodedState = null;

        if (clientSessionCode != null) {
            authSession = clientSessionCode.getClientSession();
            String relayState = clientSessionCode.getOrGenerateCode();
            String clientData = identityProvider.supportsLongStateParameter() ? AuthenticationProcessor.getClientData(context.getSession(), authSession) : null;
            encodedState = IdentityBrokerState.decoded(relayState, authSession.getClient().getId(), authSession.getClient().getClientId(), authSession.getTabId(), clientData);
        }

        KeycloakSession keycloakSession = context.getSession();
        KeycloakUriInfo keycloakUriInfo = keycloakSession.getContext().getUri();
        RealmModel realm = context.getRealm();
        String redirectUri = Urls.identityProviderAuthnResponse(keycloakUriInfo.getBaseUri(), providerAlias, realm.getName()).toString();
        return new AuthenticationRequest(keycloakSession, realm, authSession, context.getHttpRequest(), keycloakUriInfo, encodedState, redirectUri);
    }

}
