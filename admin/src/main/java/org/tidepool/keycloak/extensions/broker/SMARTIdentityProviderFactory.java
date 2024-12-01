package org.tidepool.keycloak.extensions.broker;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SMARTIdentityProviderFactory extends AbstractIdentityProviderFactory<SMARTIdentityProvider> {

    public static final String PROVIDER_ID = "smart";

    public static final String FHIR_R4 = "R4";
    public static final String[] SUPPORTED_FHIR_VERSIONS = {FHIR_R4};

    @Override
    public String getName() {
        return "SMART";
    }

    @Override
    public SMARTIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new SMARTIdentityProvider(session, new SMARTIdentityProviderConfig(model));
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new SMARTIdentityProviderConfig();
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String config) {
        return parseSMARTConfig(session, config);
    }

    protected static Map<String, String> parseSMARTConfig(KeycloakSession session, String configString) {
        OIDCConfigurationRepresentation rep;
        try {
            rep = JsonSerialization.readValue(configString, OIDCConfigurationRepresentation.class);
        } catch (IOException e) {
            throw new RuntimeException("failed to load openid connect metadata", e);
        }
        SMARTIdentityProviderConfig config = new SMARTIdentityProviderConfig();
        config.setIssuer(rep.getIssuer());
        return config.getConfig();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property().name("issuer").label("Issuer").type(ProviderConfigProperty.STRING_TYPE).required(true).add()
                .property().name("scopes").label("Scopes").type(ProviderConfigProperty.STRING_TYPE).add()
                .property().name("forwardParameters").label("Forward Parameters").type(ProviderConfigProperty.STRING_TYPE).add()
                .property().name("fhirVersion").label("FHIR Version").type(ProviderConfigProperty.LIST_TYPE).options(SUPPORTED_FHIR_VERSIONS).defaultValue(SUPPORTED_FHIR_VERSIONS[0]).required(true).add()
                .build();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void init(Config.Scope config) {
        // Load the FHIR Context on startup
        FHIRContext.getR4();
    }
}
