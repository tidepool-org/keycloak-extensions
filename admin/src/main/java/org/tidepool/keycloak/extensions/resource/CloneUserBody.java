package org.tidepool.keycloak.extensions.resource;

public class CloneUserBody {
    public String newUsername;
    public String custodianRoleName;

    public CloneUserBody() {
        this.newUsername = "";
        this.custodianRoleName = "";
    }

    public CloneUserBody(String newUsername, String custodianRoleName) {
        this.newUsername = newUsername;
        this.custodianRoleName = custodianRoleName;
    }
}
