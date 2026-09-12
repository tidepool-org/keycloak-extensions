package org.tidepool.keycloak.extensions.events;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Registers {@link RecoveryCodesCleanupEventListenerProvider}.
 *
 * <p>This is a {@linkplain #isGlobal() global} event listener, so it runs for every realm without
 * any per-realm configuration &mdash; there is no need to add {@value #PROVIDER_ID} to a realm's
 * event listeners.
 */
@AutoService(EventListenerProviderFactory.class)
public class RecoveryCodesCleanupEventListenerProviderFactory implements EventListenerProviderFactory {

    public static final String PROVIDER_ID = "tidepool-recovery-codes-cleanup";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new RecoveryCodesCleanupEventListenerProvider(session);
    }

    @Override
    public boolean isGlobal() {
        // Always active for all realms; no per-realm "eventsListeners" entry required.
        return true;
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
        return PROVIDER_ID;
    }
}
