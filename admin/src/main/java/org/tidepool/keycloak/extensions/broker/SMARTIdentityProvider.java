package org.tidepool.keycloak.extensions.broker;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Practitioner;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

public class SMARTIdentityProvider extends OIDCIdentityProvider {
    private static final Logger LOG = Logger.getLogger(SMARTIdentityProvider.class);

    public static final String FHIR_VERSION = "smart/fhir_version";
    public static final String FHIR_BASE_URL = "smart/fhir_base_url";

    private static final String[] DEFAULT_FORWARD_PARAMETERS = {"launch", "aud", "iss"};

    private final SMARTIdentityProviderConfig config;

    public SMARTIdentityProvider(KeycloakSession session, SMARTIdentityProviderConfig config) {
        super(session, discoverConfig(session, config.getIssuer()));

        this.config = config;
        getConfig().setClientId(config.getClientId());
        getConfig().setClientSecret(config.getClientSecret());
        getConfig().setDefaultScope(config.getScopes());
        getConfig().setAlias(config.getAlias());
        getConfig().setForwardParameters(withDefaultForwardParameters(config.getForwardParameters()));
        getConfig().setDisableUserInfoService(true);
    }

    @Override
    protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {
        BrokeredIdentityContext identity = super.extractIdentity(tokenResponse, accessToken, idToken);

        Practitioner practitioner = getPractitioner(idToken.getSubject(), accessToken);
        for (ContactPoint c : practitioner.getTelecom()) {
            if (c.getSystem() == ContactPoint.ContactPointSystem.EMAIL && c.getValue() != null && !c.getValue().isBlank()) {
                identity.setEmail(c.getValue());
                break;
            }
        }

        identity.setFirstName(practitioner.getNameFirstRep().getGivenAsSingleString());
        identity.setLastName(practitioner.getNameFirstRep().getFamily());

        identity.getContextData().put(FHIR_VERSION, config.getFHIRVersion());
        identity.getContextData().put(FHIR_BASE_URL, config.getIssuer());

        return identity;
    }

    private Practitioner getPractitioner(String id, String accessToken) {
        IGenericClient client = FHIRContext.getFHIRClient(config.getFHIRVersion(), config.getIssuer(), accessToken);
        Practitioner practitioner = client.read().resource(Practitioner.class).withId(id).execute();

        if (LOG.isTraceEnabled()) {
            IParser parser = FHIRContext.getR4().newJsonParser();
            LOG.tracef("Retrieved practitioner resource: %s", parser.encodeResourceToString(practitioner));
        }

        return practitioner;
    }

    private static String withDefaultForwardParameters(String params){
        if (params == null) {
            params = "";
        }

        HashSet<String> set = new HashSet<>(Arrays.asList(params.split(",")));
        set.addAll(Arrays.asList(DEFAULT_FORWARD_PARAMETERS));
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
                String detail = String.format("Unexpected response %d", response.getStatus());
                String tmp = response.asString();
                if (tmp != null) detail = tmp;

                String msg = String.format("Failed to invoke url [%s]: %s", smartConfigurationUrl, detail) ;

                throw new IdentityBrokerException(msg);
            }

            identityProviderConfig.setConfig(factory.parseConfig(session, response.asString()));
        } catch (IOException e) {
            throw new IdentityBrokerException("Unable to retrieve smart configuration");
        }

        return identityProviderConfig;
    }
}
