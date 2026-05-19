package org.tidepool.keycloak.extensions.authenticator;

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

import static org.keycloak.models.AuthenticationExecutionModel.Requirement.DISABLED;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;

/**
 * Conditional authenticator that matches when the current authentication flow
 * was started by an Application-Initiated Action (AIA) — i.e. the OIDC client
 * passed the {@code kc_action} query parameter (e.g. {@code UPDATE_PASSWORD}).
 *
 * <p>Wrap an authenticator like OTP in a CONDITIONAL subflow with this
 * condition + {@code Negate output = true} (the default) to <em>skip</em> the
 * authenticator during AIA re-auth while still requiring it on regular logins.
 */
public final class ConditionAppInitiatedActionFactory implements AuthenticatorFactory, ServerInfoAwareProviderFactory {

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = new AuthenticationExecutionModel.Requirement[]{REQUIRED, DISABLED};

    private static final String PROVIDER_ID = "condition-app-initiated-action";

    public static final String CONF_NEGATE = "negate";

    @Override
    public String getDisplayType() {
        return "Condition - App-Initiated Action";
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
        return "Matches when the authentication flow was triggered by an Application-Initiated Action (kc_action). Useful for skipping authenticators like OTP during AIA re-auth.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty negate = new ProviderConfigProperty();
        negate.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negate.setName(CONF_NEGATE);
        negate.setLabel("Negate output");
        negate.setDefaultValue(Boolean.toString(true));
        negate.setHelpText("Apply a NOT to the check result. When this is true, the condition evaluates to true when the flow was NOT triggered by an Application-Initiated Action.");
        return List.of(negate);
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new ConditionAppInitiatedAction();
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
    public Map<String, String> getOperationalInfo() {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "dev-snapshot";
        }
        return Map.of("Version", version);
    }
}
