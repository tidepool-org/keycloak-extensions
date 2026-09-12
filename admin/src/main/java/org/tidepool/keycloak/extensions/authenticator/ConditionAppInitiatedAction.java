package org.tidepool.keycloak.extensions.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public class ConditionAppInitiatedAction implements ConditionalAuthenticator {

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        boolean isAia = isAppInitiatedAction(context.getAuthenticationSession());

        // Negate defaults to TRUE to match the factory's UI default. Without this,
        // admins who add the condition but never click Save in the config dialog
        // get the opposite of the documented behavior — the config map is empty
        // and Boolean.parseBoolean(null) is false.
        boolean negateOutput = true;
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig != null && authConfig.getConfig() != null) {
            String configured = authConfig.getConfig().get(ConditionAppInitiatedActionFactory.CONF_NEGATE);
            if (configured != null) {
                negateOutput = Boolean.parseBoolean(configured);
            }
        }

        return negateOutput != isAia;
    }

    /**
     * The OIDC AuthorizationEndpoint stores the {@code kc_action} query
     * parameter on the auth session as a client note before the browser flow
     * starts. The note is also re-set during action execution. Either presence
     * is enough to identify an AIA-driven flow.
     */
    private static boolean isAppInitiatedAction(AuthenticationSessionModel authSession) {
        if (authSession == null) {
            return false;
        }
        String kcAction = authSession.getClientNote(Constants.KC_ACTION);
        if (kcAction == null || kcAction.isEmpty()) {
            kcAction = authSession.getClientNote(Constants.KC_ACTION_EXECUTING);
        }
        return kcAction != null && !kcAction.isEmpty();
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
