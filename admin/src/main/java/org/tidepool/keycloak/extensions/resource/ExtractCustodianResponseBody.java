package org.tidepool.keycloak.extensions.resource;

import org.keycloak.representations.idm.UserRepresentation;

public class ExtractCustodianResponseBody {
    // custodian is the new account that is created when extracting a custodian
    // from a profile with a fake child.
    public UserRepresentation custodian;

    // custodialEmail is the new email of the original account that had a
    // custodian extracted from it.
    public String custodialEmail;

    public ExtractCustodianResponseBody(UserRepresentation custodian, String custodialEmail) {
        this.custodian = custodian;
        this.custodialEmail = custodialEmail;
    }
}
