package org.tidepool.keycloak.extensions.login;

import java.net.URI;
import java.util.Locale;

import jakarta.ws.rs.core.Response;

import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.Theme;
import org.tidepool.keycloak.extensions.model.RoleBean;

public class TidepoolLoginFormsProvider extends FreeMarkerLoginFormsProvider {

    private static final String FORM_ATTRIBUTE_ROLE = "role";

    private static final String REGISTER_FORM = "register.ftl";
    private static final String REGISTER_FORM_CLINICIAN = "register-clinician.ftl";
    private static final String REGISTER_FORM_PERSONAL = "register-personal.ftl";

    public TidepoolLoginFormsProvider(KeycloakSession session) {
        super(session);
    }

    @Override
    protected Response processTemplate(Theme theme, String templateName, Locale locale) {
        URI baseUri = super.prepareBaseUriBuilder(false).build();

        RoleBean roleBean = new RoleBean(realm, baseUri, context, authenticationSession);
        attributes.put(FORM_ATTRIBUTE_ROLE, roleBean);

        if (templateName == REGISTER_FORM) {
            if (roleBean.hasClinicianRole()) {
                templateName = REGISTER_FORM_CLINICIAN;
            } else {
                templateName = REGISTER_FORM_PERSONAL;
            }
        }

        return super.processTemplate(theme, templateName, locale);
    }
}
