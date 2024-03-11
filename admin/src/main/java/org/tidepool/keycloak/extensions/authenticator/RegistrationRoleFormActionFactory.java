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

public final class RegistrationRoleFormActionFactory implements FormActionFactory, ServerInfoAwareProviderFactory {

    private static final String ID = "tidepool-registration-role";

    private static final Requirement[] REQUIREMENT_CHOICES = { Requirement.REQUIRED, Requirement.DISABLED };

    @Override
    public FormAction create(KeycloakSession session) {
        return new RegistrationRoleFormAction();
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
        return "Tidepool Registration Role";
    }

    @Override
    public String getReferenceCategory() {
        return "registration-role";
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
        return "Applies the role based upon the associated auth note set by the tidepool-registration-role-discovery authenticator";
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
