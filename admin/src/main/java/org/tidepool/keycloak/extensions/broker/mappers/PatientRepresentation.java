package org.tidepool.keycloak.extensions.broker.mappers;

import org.hl7.fhir.r4.model.Patient;

import java.time.Instant;

public class PatientRepresentation {
    public String id;
    public String firstName;
    public String lastName;
    public String mrn;
    public Long timestamp;

    public PatientRepresentation() { }

    public PatientRepresentation(Patient patient) {
        id = patient.getId();
        firstName = patient.getNameFirstRep().getGivenAsSingleString();
        lastName = patient.getNameFirstRep().getFamily();
        mrn = patient.getIdentifierFirstRep().getValue();

        // Capture the time when the patient representation was instantiated
        // to allow for LRU pruning if needed
        timestamp = Instant.now().getEpochSecond();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

}

