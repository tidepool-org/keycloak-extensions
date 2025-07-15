package org.tidepool.keycloak.extensions.broker.mappers;

import org.hl7.fhir.r4.model.Identifier.*;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.jboss.logging.Logger;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.stream.Collectors;

public class PatientRepresentation {

    private static final Logger LOG = Logger.getLogger(PatientRepresentation.class);


    private static SimpleDateFormat dobFormat = new SimpleDateFormat("yyyy-MM-dd");

    public String id;
    public String firstName;
    public String lastName;
    public String mrn;
    public Long timestamp;
    public String dob;

    public PatientRepresentation() { }

    public PatientRepresentation(Patient patient, String mrnIdentifierType) {
        id = patient.getId();
        firstName = patient.getNameFirstRep().getGivenAsSingleString();
        lastName = patient.getNameFirstRep().getFamily();
        mrn = getIdentifier(patient, mrnIdentifierType);
        dob = patient.getBirthDate() == null ? null : dobFormat.format(patient.getBirthDate());

        // Capture the time when the patient representation was instantiated
        // to allow for LRU pruning if needed
        timestamp = Instant.now().getEpochSecond();
    }

    private static String getIdentifier(Patient patient, String mrnIdentifierType) {
        Identifier id = null;
        for (Identifier t : patient.getIdentifier()) {
            if (!t.hasType() || !mrnIdentifierType.equalsIgnoreCase(t.getType().getText())) {
                continue;
            }
            id = chooseId(id, t);
        }
        if (id == null) {
            String identifiers = patient.getIdentifier().stream()
                    .map(PatientRepresentation::identifierRepresentation)
                    .filter(i -> i != null && !i.isBlank())
                    .distinct().
                    collect(Collectors.joining(", "));
            LOG.warnf("unable to find %s patient identifier in %s", mrnIdentifierType, identifiers);
            return null;
        }

        return id.getValue();
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

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    private static Identifier chooseId(Identifier oldId, Identifier newId) {
        if (oldId == null) {
            return newId;
        }
        if (newId == null) {
            return oldId;
        }
        return isPreferred(newId.getUse(), oldId.getUse()) ? newId : oldId;
    }

    private static boolean isPreferred(IdentifierUse newUse, IdentifierUse oldUse) {
        if (newUse == null && oldUse == null || newUse == oldUse) {
            return false;
        }
        if (newUse == null) {
            return true;
        }
        switch (newUse) {
            case NULL:
            case SECONDARY:
                return !existsInList(oldUse, IdentifierUse.OFFICIAL, Identifier.IdentifierUse.USUAL);
            case OFFICIAL:
                return !existsInList(oldUse, IdentifierUse.USUAL);
            case OLD:
            case TEMP:
                return !existsInList(oldUse, IdentifierUse.OFFICIAL, IdentifierUse.SECONDARY, IdentifierUse.USUAL);
            case USUAL:
                return true;
            default:
                return false;
        }
    }

    private static boolean existsInList(IdentifierUse oldUse, IdentifierUse... values) {
        for (IdentifierUse value : values) {
            if (value == oldUse) {
                return true;
            }
        }
        return false;
    }

    private static String identifierRepresentation(Identifier identifier) {
        if (identifier == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        if (identifier.hasType() && identifier.getType().getText() != null) {
            builder.append(identifier.getType().getText());
            builder.append(" ");
        }
        if (identifier.hasSystem() && identifier.getSystem() != null) {
            builder.append(identifier.getSystem());
            builder.append(" ");
        }
        if (identifier.getValue() != null) {
            builder.append(identifier.getValue());
        }
        if (builder.length() == 0) {
            return null;
        }
        return builder.toString();
    }
}

