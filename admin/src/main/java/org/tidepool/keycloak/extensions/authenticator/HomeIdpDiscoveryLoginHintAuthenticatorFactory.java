package org.tidepool.keycloak.extensions.authenticator;

import de.sventorben.keycloak.authentication.hidpd.AbstractHomeIdpDiscoveryAuthenticatorFactory;
import de.sventorben.keycloak.authentication.hidpd.OperationalInfo;
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

import static org.keycloak.models.AuthenticationExecutionModel.Requirement.ALTERNATIVE;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.DISABLED;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

public final class HomeIdpDiscoveryLoginHintAuthenticatorFactory implements AuthenticatorFactory, ServerInfoAwareProviderFactory {

    private static final String PROVIDER_ID = "home-idp-discovery-login-hint";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{REQUIRED, ALTERNATIVE, DISABLED};

    private final AbstractHomeIdpDiscoveryAuthenticatorFactory.DiscovererConfig discovererConfig;

    public HomeIdpDiscoveryLoginHintAuthenticatorFactory() {
        this.discovererConfig = new EmailHomeIdpDiscoveryAuthenticatorFactoryDiscovererConfig();
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
        return (new EmailHomeIdpDiscoveryAuthenticatorFactoryDiscovererConfig()).getProperties();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new HomeIdpDiscoveryLoginHintAuthenticator(discovererConfig);
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public final void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public final void close() {
    }

    @Override
    public final Map<String, String> getOperationalInfo() {
        return OperationalInfo.get();
    }

    @Override
    public String getDisplayType() {
        return "Home IdP Discovery (Login Hint Redirector)";
    }

    @Override
    public String getReferenceCategory() {
        return "Authorization";
    }

    @Override
    public String getHelpText() {
        return "Redirects users to their home identity provider based on the provided login hint";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
