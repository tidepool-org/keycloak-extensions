package org.tidepool.keycloak.extensions.resource;

public class CloneUserBody {
    public String newUsername;

    public CloneUserBody() {
        this.newUsername = "";
    }

    public CloneUserBody(String newUsername) {
        this.newUsername = newUsername;
    }
}
