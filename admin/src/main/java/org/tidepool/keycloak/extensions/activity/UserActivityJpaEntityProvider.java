package org.tidepool.keycloak.extensions.activity;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

/**
 * Contributes the {@link UserActivityEventEntity} mapping and its Liquibase changelog to Keycloak's
 * shared JPA datasource, so the outbox table is created and migrated alongside Keycloak's own schema.
 */
public class UserActivityJpaEntityProvider implements JpaEntityProvider {

    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(UserActivityEventEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/tidepool-user-activity-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return UserActivityJpaEntityProviderFactory.ID;
    }

    @Override
    public void close() {
    }
}
