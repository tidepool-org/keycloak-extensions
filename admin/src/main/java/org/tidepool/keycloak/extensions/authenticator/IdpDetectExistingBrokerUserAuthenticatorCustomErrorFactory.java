package org.tidepool.keycloak.extensions.authenticator;

import java.util.Collections;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class IdpDetectExistingBrokerUserAuthenticatorCustomErrorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "idp-existing-user-custom-error";

    public static final String CONF_ERROR_MESSAGE = "errorMessage";

    @Override
    public Authenticator create(KeycloakSession session) {
        return new IdpDetectExistingBrokerUserAuthenticatorCustomError();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getReferenceCategory() {
        return "detectExistingBrokerUser";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
                AuthenticationExecutionModel.Requirement.REQUIRED,
                AuthenticationExecutionModel.Requirement.DISABLED
        };
    }

    @Override
    public String getDisplayType() {
        return "Detect existing broker user (custom error)";
    }

    @Override
    public String getHelpText() {
        return "Detect if there is an existing Keycloak account with same email like identity provider. If no, throw the configured error.";
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty errorMessage = new ProviderConfigProperty();
        errorMessage.setType(ProviderConfigProperty.STRING_TYPE);
        errorMessage.setName(CONF_ERROR_MESSAGE);
        errorMessage.setLabel("Error message");
        errorMessage.setHelpText("Display customer error message");

        return List.of(errorMessage);
    }
}