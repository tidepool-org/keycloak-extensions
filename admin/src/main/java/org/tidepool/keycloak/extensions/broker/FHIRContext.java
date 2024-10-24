package org.tidepool.keycloak.extensions.broker;

import ca.uhn.fhir.context.FhirContext;

public class FHIRContext {
    // Singleton instance - the creation of this object is expensive
    private static final FhirContext R4 = FhirContext.forR4();

    private FHIRContext() {}

    public static FhirContext getR4() {
        return R4;
    }
}
