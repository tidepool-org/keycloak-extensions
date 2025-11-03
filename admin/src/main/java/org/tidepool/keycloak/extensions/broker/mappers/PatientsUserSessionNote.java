package org.tidepool.keycloak.extensions.broker.mappers;

import org.hl7.fhir.r4.model.Patient;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;

public class PatientsUserSessionNote extends HashMap<String, PatientRepresentation> {

    public PatientsUserSessionNote() {
        super();
    }

    public void addPatient(String correlationId, Patient patient, String mrnIdentifierType) {
        this.put(correlationId, new PatientRepresentation(patient, mrnIdentifierType));
    }

    public String serializeAsString() throws IOException {
        return JsonSerialization.writeValueAsString(this);
    }

    public static PatientsUserSessionNote deserializeFromString(String value) throws IOException {
        if (value == null || value.isBlank()) {
            return new PatientsUserSessionNote();
        }

        return JsonSerialization.readValue(value, PatientsUserSessionNote.class);
    }
}
