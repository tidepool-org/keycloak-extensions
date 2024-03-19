package org.tidepool.keycloak.extensions.login;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.LoginFormsProviderFactory;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProviderFactory;
import org.keycloak.models.KeycloakSession;

import com.google.auto.service.AutoService;

@AutoService(LoginFormsProviderFactory.class)
public class TidepoolLoginFormsProviderFactory extends FreeMarkerLoginFormsProviderFactory {

    @Override
    public LoginFormsProvider create(KeycloakSession session) {
        return new TidepoolLoginFormsProvider(session);
    }
}
