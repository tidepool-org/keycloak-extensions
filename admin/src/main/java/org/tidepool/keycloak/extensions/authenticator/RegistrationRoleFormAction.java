package org.tidepool.keycloak.extensions.authenticator;

import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.tidepool.keycloak.extensions.model.RoleBean;

final class RegistrationRoleFormAction implements FormAction {

    private static final String EVENT_DETAIL_ROLE = "role";

    @Override
    public void close() {
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
    }

    @Override
    public void validate(ValidationContext context) {
        String role = context.getAuthenticationSession().getAuthNote(RoleBean.AUTH_NOTE_ROLE);
        if (RoleBean.ROLES_SET.contains(role)) {
            context.getEvent().detail(EVENT_DETAIL_ROLE, role);
        }

        context.success();
    }

    @Override
    public void success(FormContext context) {
        String role = context.getAuthenticationSession().getAuthNote(RoleBean.AUTH_NOTE_ROLE);
        if (RoleBean.ROLES_SET.contains(role)) {
            RoleModel roleModel = context.getRealm().getRole(role);
            if (roleModel != null) {
                context.getUser().grantRole(roleModel);
            }
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
