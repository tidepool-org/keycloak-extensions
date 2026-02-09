package org.tidepool.keycloak.extensions.authenticator;


import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.EmailValidationUtil;

public class RequireValidBrokerEmail extends AbstractIdpAuthenticator {

    private static final Logger LOG = Logger.getLogger(RequireValidBrokerEmail.class);

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        String email = brokerContext.getEmail();
        boolean isValid = EmailValidationUtil.isValidEmail(email);

        LOG.debugf("broker email '%s' for user '%s' of provider '%s' is%s valid", email, brokerContext.getUsername(), brokerContext.getIdpConfig().getAlias(), isValid ? "" : " not");

        if (isValid) {
            context.success();
            return;
        }

        context.attempted();
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {
        return false;
    }
}
