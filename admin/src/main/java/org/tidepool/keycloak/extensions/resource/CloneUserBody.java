package org.tidepool.keycloak.extensions.resource;

public class CloneUserBody {
    // newUsername is the username (email) the existing user, which
    // contains a fake child, will become - the newly created parent
    // will have the existing user's previous username / email.
    // This should conform to the unclaimed user email format.
    public String newUsername;

    // custodianRoleName is the role the newly created parent of the
    // existing user will receive - this should be some custodian
    // type role.
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
