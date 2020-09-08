package org.tidepool.keycloak.extensions.resource;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
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
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        resource.setup();
        return resource;
    }

    @Override
    public void close() {

    }

}
