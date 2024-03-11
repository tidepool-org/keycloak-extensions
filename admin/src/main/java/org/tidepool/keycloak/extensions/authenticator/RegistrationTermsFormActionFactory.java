package org.tidepool.keycloak.extensions.authenticator;

import java.util.List;
import java.util.Map;

import org.keycloak.Config.Scope;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

public final class RegistrationTermsFormActionFactory implements FormActionFactory, ServerInfoAwareProviderFactory {

    private static final String ID = "tidepool-registration-terms";

    private static final Requirement[] REQUIREMENT_CHOICES = { Requirement.REQUIRED, Requirement.DISABLED };

    @Override
    public FormAction create(KeycloakSession session) {
        return new RegistrationTermsFormAction();
    }

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayType() {
        return "Tidepool Registration Terms";
    }

    @Override
    public String getReferenceCategory() {
        return "registration-terms";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Determines whether the terms have been accepted in the form and sets the terms_and_conditions attribute on the user";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return null;
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "dev-snapshot";
        }
        return Map.of("Version", version);
    }
}
