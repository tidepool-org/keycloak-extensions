package org.tidepool.keycloak.extensions.authenticator;

import de.sventorben.keycloak.authentication.hidpd.AbstractHomeIdpDiscoveryAuthenticatorFactory;
import de.sventorben.keycloak.authentication.hidpd.discovery.email.EmailHomeIdpDiscoveryAuthenticatorFactoryDiscovererConfig;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.List;
import java.util.Map;

import static org.keycloak.models.AuthenticationExecutionModel.Requirement.*;

public final class HomeIdpDiscoveryMatchingEmailAuthenticatorFactory implements AuthenticatorFactory, ServerInfoAwareProviderFactory {

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{REQUIRED, DISABLED};

    private static final String PROVIDER_ID = "home-idp-discovery-matching-email";

    public static final String CONF_NEGATE = "negate";

    private final AbstractHomeIdpDiscoveryAuthenticatorFactory.DiscovererConfig discovererConfig;

    public HomeIdpDiscoveryMatchingEmailAuthenticatorFactory() {
        this.discovererConfig = new EmailHomeIdpDiscoveryAuthenticatorFactoryDiscovererConfig();
    }

    @Override
    public String getDisplayType() {
        return "Home IdP Discovery (Check Email Matches Provider)";
    }

    @Override
    public String getReferenceCategory() {
        return "Authorization";
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
    public String getHelpText() {
        return "Checks if the email attribute from the provider matches the configured SSO domains";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = (new EmailHomeIdpDiscoveryAuthenticatorFactoryDiscovererConfig()).getProperties();

        ProviderConfigProperty negateOutput = new ProviderConfigProperty();
        negateOutput.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negateOutput.setName(CONF_NEGATE);
        negateOutput.setLabel("Negate output");
        negateOutput.setHelpText("Apply a NOT to the check result. When this is true, then the condition will evaluate to true if the email does NOT match the discovered idp.");

        properties.add(negateOutput);
        return properties;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new HomeIdpDiscoveryMatchingEmailAuthenticator(this.discovererConfig);
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
}
