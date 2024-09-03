package org.tidepool.keycloak.extensions.authenticator;

import de.sventorben.keycloak.authentication.hidpd.AbstractHomeIdpDiscoveryAuthenticatorFactory;
import de.sventorben.keycloak.authentication.hidpd.discovery.spi.HomeIdpDiscoverer;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.List;

public class HomeIdpDiscoveryMatchingEmailAuthenticator implements ConditionalAuthenticator {
    private static final Logger LOG = Logger.getLogger(HomeIdpDiscoveryMatchingEmailAuthenticator.class);

    private static final String LINKING_IDENTITY_PROVIDER_NOTE = "LINKING_IDENTITY_PROVIDER";

    private final AbstractHomeIdpDiscoveryAuthenticatorFactory.DiscovererConfig discovererConfig;

    HomeIdpDiscoveryMatchingEmailAuthenticator(AbstractHomeIdpDiscoveryAuthenticatorFactory.DiscovererConfig discovererConfig) {
        this.discovererConfig = discovererConfig;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext authenticationFlowContext) {
        AuthenticationSessionModel clientSession = authenticationFlowContext.getAuthenticationSession();
        String linkingNote = clientSession.getAuthNote(LINKING_IDENTITY_PROVIDER_NOTE);

        if (linkingNote != null) {
            // Always assume the email matches the linking provider
            // if we're in a client initiated linking flow
            LOG.debug("Skipping email match check in client initiated account linking session");
            return true;
        }

        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(clientSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
        if (serializedCtx == null) {
            throw new AuthenticationFlowException("Not found serialized context in clientSession", AuthenticationFlowError.IDENTITY_PROVIDER_ERROR);
        }
        BrokeredIdentityContext brokerContext = serializedCtx.deserialize(authenticationFlowContext.getSession(), clientSession);
        String providerId = brokerContext.getIdpConfig().getProviderId();


        HomeIdpDiscoverer discoverer = authenticationFlowContext.getSession().getProvider(HomeIdpDiscoverer.class, discovererConfig.getProviderId());
        List<IdentityProviderModel> homeIdps = discoverer.discoverForUser(authenticationFlowContext, serializedCtx.getEmail());
        boolean matchesEmail = homeIdps.stream().anyMatch(x -> x.isEnabled() && x.getProviderId().equals(providerId));

        LOG.debugf("Email %s %s identity provider %s", serializedCtx.getEmail(), matchesEmail ? "matches" : "doesn't match", providerId);

        AuthenticatorConfigModel authConfig = authenticationFlowContext.getAuthenticatorConfig();
        if (authConfig!=null && authConfig.getConfig()!=null) {
            boolean negateOutput = Boolean.parseBoolean(authConfig.getConfig().get(HomeIdpDiscoveryMatchingEmailAuthenticatorFactory.CONF_NEGATE));
            return negateOutput != matchesEmail;
        }

        return matchesEmail;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Not used
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Not used
    }

    @Override
    public void close() {
        // Not used
    }
}

