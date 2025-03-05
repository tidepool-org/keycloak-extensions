package org.tidepool.keycloak.extensions.broker;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import org.keycloak.broker.provider.IdentityBrokerException;

public class FHIRContext {
    // Singleton instance - the creation of this object is expensive
    private static final FhirContext R4 = FhirContext.forR4();

    private FHIRContext() {}

    public static FhirContext getR4() {
        return R4;
    }

    public static IGenericClient getFHIRClient(String version, String baseUrl, String accessToken) {
        if (!SMARTIdentityProviderFactory.FHIR_R4.equals(version)) {
            throw new IdentityBrokerException("Unsupported FHIR Version: " + version);
        }

        FhirContext ctx = FHIRContext.getR4();
        IClientInterceptor authInterceptor = new BearerTokenAuthInterceptor(accessToken);
        IGenericClient client = ctx.newRestfulGenericClient(baseUrl);
        client.registerInterceptor(authInterceptor);

        return client;
    }
}
