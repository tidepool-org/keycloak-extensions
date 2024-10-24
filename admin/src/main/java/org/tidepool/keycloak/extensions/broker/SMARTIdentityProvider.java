package org.tidepool.keycloak.extensions.broker;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Patient;
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

    private static final String[] defaultForwardParameters = {"launch", "aud", "iss"};

    public static  final String FHIR_R4 = "R4";

    private final SMARTIdentityProviderConfig config;

    public SMARTIdentityProvider(KeycloakSession session, SMARTIdentityProviderConfig config) {
        super(session, discoverConfig(session, config.getIssuer()));
        getConfig().setClientId(config.getClientId());
        getConfig().setClientSecret(config.getClientSecret());
        getConfig().setDefaultScope(config.getScopes());
        getConfig().setAlias(config.getAlias());
        getConfig().setForwardParameters(withDefaultForwardParameters(config.getForwardParameters()));
        getConfig().setDisableUserInfoService(true);

        this.config = config;
    }

    @Override
    protected BrokeredIdentityContext extractIdentity(AccessTokenResponse tokenResponse, String accessToken, JsonWebToken idToken) throws IOException {
        BrokeredIdentityContext identity = super.extractIdentity(tokenResponse, accessToken, idToken);

        Practitioner practitioner = getPractitioner(idToken.getSubject(), accessToken);
        for (ContactPoint c : practitioner.getTelecom()) {
            if (c.getSystem() == ContactPoint.ContactPointSystem.EMAIL && c.getValue() != null && !c.getValue().isEmpty()) {
                identity.setEmail(c.getValue());
                break;
            }
        }

        identity.setFirstName(practitioner.getNameFirstRep().getGivenAsSingleString());
        identity.setLastName(practitioner.getNameFirstRep().getFamily());

        return identity;
    }

    private Practitioner getPractitioner(String id, String accessToken) {
        if (!FHIR_R4.equals(config.getFHIRVersion())) {
            throw new IdentityBrokerException("Unsupported FHIR Version: " + config.getFHIRVersion());
        }

        FhirContext ctx = FHIRContext.getR4();
        IClientInterceptor authInterceptor = new BearerTokenAuthInterceptor(accessToken);
        IGenericClient client = ctx.newRestfulGenericClient(config.getIssuer());
        client.registerInterceptor(authInterceptor);
        Practitioner practitioner = client.read().resource(Practitioner.class).withId(id).execute();

        if (LOG.isTraceEnabled()) {
            IParser parser = ctx.newJsonParser();
            String serialized = parser.encodeResourceToString(practitioner);
            LOG.tracef("Retrieved practitioner resource: " + serialized);
        }

        return practitioner;
    }

    private Patient getPatient(String id, String accessToken) {
        if (!FHIR_R4.equals(config.getFHIRVersion())) {
            throw new IdentityBrokerException("Unsupported FHIR Version: " + config.getFHIRVersion());
        }

        FhirContext ctx = FHIRContext.getR4();
        IClientInterceptor authInterceptor = new BearerTokenAuthInterceptor(accessToken);
        IGenericClient client = ctx.newRestfulGenericClient(config.getIssuer());
        return client.read().resource(Patient.class).withId(id).execute();
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
