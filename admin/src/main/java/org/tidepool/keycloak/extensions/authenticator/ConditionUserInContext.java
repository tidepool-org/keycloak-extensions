package org.tidepool.keycloak.extensions.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.*;

public class ConditionUserInContext implements ConditionalAuthenticator {

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        boolean isUserSetInContext = context.getUser() != null;
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig!=null && authConfig.getConfig()!=null) {
            boolean negateOutput = Boolean.parseBoolean(authConfig.getConfig().get(ConditionUserInContextFactory.CONF_NEGATE));
            return negateOutput != isUserSetInContext;
        }

        return isUserSetInContext;
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

