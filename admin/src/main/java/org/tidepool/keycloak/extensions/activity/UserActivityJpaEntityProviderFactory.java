package org.tidepool.keycloak.extensions.activity;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Registers {@link UserActivityJpaEntityProvider}. The {@link #ID} is also used as the changelog's
 * factory id so Keycloak tracks this extension's migrations independently of the core schema.
 */
@AutoService(JpaEntityProviderFactory.class)
public class UserActivityJpaEntityProviderFactory implements JpaEntityProviderFactory {

    public static final String ID = "tidepool-user-activity";

    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new UserActivityJpaEntityProvider();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }
}
