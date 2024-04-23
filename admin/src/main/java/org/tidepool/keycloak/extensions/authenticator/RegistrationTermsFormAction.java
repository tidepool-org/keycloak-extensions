package org.tidepool.keycloak.extensions.authenticator;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.tidepool.keycloak.extensions.model.RoleBean;
import org.tidepool.keycloak.extensions.services.messages.Messages;

final class RegistrationTermsFormAction implements FormAction {

    private static final String FORM_TERMS = "terms";

    private static final String FORM_TERMS_ON = "on";

    private static final String USER_ATTRIBUTE_TERMS_AND_CONDITIONS = "terms_and_conditions";

    @Override
    public void close() {
    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
    }

    @Override
    public void validate(ValidationContext context) {

        // TEMPORARY: Currently only for Clinician registration which includes TOS/PP
        // agreement on clinician registration form. Remove once TOS/PP agreement
        // included on personal registration form.
        if (!RoleBean.hasClinicianRoleFromAuthenticationSession(context.getAuthenticationSession())) {
            context.success();
            return;
        }

        context.getEvent().detail(Details.REGISTER_METHOD, "form");

        MultivaluedMap<String, String> formParameters = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();

        if (!FORM_TERMS_ON.equalsIgnoreCase(formParameters.getFirst(FORM_TERMS))) {
            errors.add(new FormMessage(FORM_TERMS, Messages.TERMS_NOT_ACCEPTED));
        }

        if (errors.size() > 0) {
            formParameters.remove(FORM_TERMS);
            context.error(Errors.INVALID_REGISTRATION);
            context.validationError(formParameters, errors);
        } else {
            context.success();
        }
    }

    @Override
    public void success(FormContext context) {

        // TEMPORARY: Currently only for Clinician registration which includes TOS/PP
        // agreement on clinician registration form. Remove once TOS/PP agreement
        // included on personal registration form.
        if (!RoleBean.hasClinicianRoleFromRealmUser(context.getRealm(), context.getUser())) {
            return;
        }

        Long secondsSinceEpoch = java.time.Instant.now().getEpochSecond();
        context.getUser().setAttribute(USER_ATTRIBUTE_TERMS_AND_CONDITIONS, List.of(String.valueOf(secondsSinceEpoch)));
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
