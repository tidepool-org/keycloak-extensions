package org.tidepool.keycloak.extensions.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.tidepool.keycloak.extensions.model.RoleBean;
import org.tidepool.keycloak.extensions.roles.UserRolePromptRequiredAction;

final class RegistrationRoleDiscoveryAuthenticator implements Authenticator {

    @Override
    public void close() {
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        // Reset APP_INITIATED_FLOW so restart redirects to login, not registration
        context.getAuthenticationSession().setClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW, null);

        String role = context.getUriInfo().getQueryParameters().getFirst(RoleBean.PARAMETER_ROLE);
        if (RoleBean.ROLES_SET.contains(role)) {
            context.getAuthenticationSession().setAuthNote(RoleBean.AUTH_NOTE_ROLE, role);
            context.success();
        } else {
            context.challenge(context.form().createForm(UserRolePromptRequiredAction.ROLES_FORM_FTL));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        String role = context.getHttpRequest().getDecodedFormParameters().getFirst(RoleBean.PARAMETER_ROLE);
        if (RoleBean.ROLES_SET.contains(role)) {
            context.getAuthenticationSession().setAuthNote(RoleBean.AUTH_NOTE_ROLE, role);
            context.success();
        } else {
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
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
}
