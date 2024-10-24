package org.tidepool.keycloak.extensions.broker;

import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class SMARTIdentityProvider extends OIDCIdentityProvider {

    private static final String[] defaultForwardParameters = {"launch", "aud", "iss"};

    private final String fhirVersion;

    public SMARTIdentityProvider(KeycloakSession session, SMARTIdentityProviderConfig config) {
        super(session, discoverConfig(session, config.getIssuer()));
        getConfig().setIssuer(config.getIssuer());
        getConfig().setClientId(config.getClientId());
        getConfig().setClientSecret(config.getClientSecret());
        getConfig().setDefaultScope(config.getScopes());
        getConfig().setAlias(config.getAlias());
        getConfig().setForwardParameters(withDefaultForwardParameters(config.getForwardParameters()));

        fhirVersion = config.getFHIRVersion();
    }

    private static String withDefaultForwardParameters(String params){
        if (params == null) {
            params = "";
        }

        HashSet<String> set = new HashSet<>(Arrays.asList(params.split(",")));
        set.addAll(Arrays.asList(defaultForwardParameters));
        return String.join(",", set.stream().map(String::trim).toArray(String[]::new));
    }

    private static OIDCIdentityProviderConfig discoverConfig(KeycloakSession session, String issuer) {
        OIDCIdentityProviderFactory factory = new OIDCIdentityProviderFactory();
        OIDCIdentityProviderConfig identityProviderConfig = factory.createConfig();

        if (issuer == null || issuer.isEmpty()) {
            return identityProviderConfig;
        }

        if (!issuer.endsWith("/")) {
            issuer = issuer + "/";
        }

        String smartConfigurationUrl = issuer + ".well-known/smart-configuration";
        SimpleHttp request = SimpleHttp.doGet(smartConfigurationUrl, session).header("Accept", "application/fhir+json");

        try {
            SimpleHttp.Response response = request.asResponse();
            if (response.getStatus() != 200) {
                String msg = "failed to invoke url [" + smartConfigurationUrl + "]";
                String tmp = response.asString();
                if (tmp != null) msg = tmp;

                throw new IdentityBrokerException("Failed to invoke url [" + smartConfigurationUrl + "]: " + msg);
            }

            identityProviderConfig.setConfig(factory.parseConfig(session, response.asString()));
        } catch (IOException e) {
            throw new IdentityBrokerException("Unable to retrieve smart configuration");
        }

        return identityProviderConfig;
    }
}
