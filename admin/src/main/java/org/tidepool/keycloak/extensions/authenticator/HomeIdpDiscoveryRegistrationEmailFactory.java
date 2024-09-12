package org.tidepool.keycloak.extensions.authenticator;

import de.sventorben.keycloak.authentication.hidpd.AbstractHomeIdpDiscoveryAuthenticatorFactory;
import de.sventorben.keycloak.authentication.hidpd.discovery.email.EmailHomeIdpDiscoveryAuthenticatorFactoryDiscovererConfig;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.List;
import java.util.Map;

import static org.keycloak.models.AuthenticationExecutionModel.Requirement.DISABLED;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

public final class HomeIdpDiscoveryRegistrationEmailFactory implements FormActionFactory, ServerInfoAwareProviderFactory {

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{REQUIRED, DISABLED};

    public static final String PROVIDER_ID = "registration-email-idp-action";

    private final AbstractHomeIdpDiscoveryAuthenticatorFactory.DiscovererConfig discovererConfig;

    public HomeIdpDiscoveryRegistrationEmailFactory() {
        this.discovererConfig = new EmailHomeIdpDiscoveryAuthenticatorFactoryDiscovererConfig();
    }

    @Override
    public String getHelpText() {
        return "Validates that email domain is not bound to an IDP.";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return this.discovererConfig.getProperties();
    }

    @Override
    public void init(Config.Scope config) { }

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
    public Map<String, String> getOperationalInfo() {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "dev-snapshot";
        }
        return Map.of("Version", version);
    }

    @Override
    public String getDisplayType() {
        return "Home IdP Discovery (Email Validation)";
    }

    @Override
    public String getReferenceCategory() {
        return "Authorization";
    }

    @Override
    public FormAction create(KeycloakSession session) {
        return new HomeIdpDiscoveryRegistrationEmail(this.discovererConfig);
    }
}
