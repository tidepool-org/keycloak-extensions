package org.tidepool.keycloak.extensions.resource;

import org.keycloak.models.*;
import org.keycloak.services.resource.RealmResourceProvider;

public class TidepoolAdminResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public TidepoolAdminResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        TidepoolAdminResource resource = new TidepoolAdminResource(session);
        resource.setup();
        return resource;
    }

    @Override
    public void close() {

    }

}
