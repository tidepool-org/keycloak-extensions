package org.tidepool.keycloak.extensions.activity;

import com.google.auto.service.AutoService;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

/**
 * Registers {@link UserActivityEventListenerProvider} and schedules the outbox {@link UserActivityCleanupTask}.
 *
 * <p>This is a {@linkplain #isGlobal() global} listener, so it runs for every realm with no per-realm
 * "events listeners" configuration. Retention and pruning are configurable via SPI options, e.g.:
 *
 * <pre>
 *   spi-events-listener-tidepool-user-activity-retention-hours=168       # &lt;= 0 disables age pruning
 *   spi-events-listener-tidepool-user-activity-max-rows=5000000          # &lt;= 0 disables the size cap
 *   spi-events-listener-tidepool-user-activity-cleanup-interval-minutes=60
 * </pre>
 */
@AutoService(EventListenerProviderFactory.class)
public class UserActivityEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final Logger LOG = Logger.getLogger(UserActivityEventListenerProviderFactory.class);

    public static final String PROVIDER_ID = "tidepool-user-activity";
    public static final String CLEANUP_TASK_NAME = "tidepool-user-activity-cleanup";

    private static final long DEFAULT_RETENTION_HOURS = 168L; // one week
    private static final long DEFAULT_MAX_ROWS = 5_000_000L;
    private static final long DEFAULT_CLEANUP_INTERVAL_MINUTES = 60L;

    private long retentionMillis;
    private long maxRows;
    private long cleanupIntervalMillis;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new UserActivityEventListenerProvider(session);
    }

    @Override
    public boolean isGlobal() {
        // Always active for all realms; no per-realm "eventsListeners" entry required.
        return true;
    }

    @Override
    public void init(Config.Scope config) {
        // A non-positive retention disables age-based pruning (consistent with max-rows), rather than
        // collapsing the cutoff to "now" and wiping the table on every sweep.
        long retentionHours = config.getLong("retention-hours", DEFAULT_RETENTION_HOURS);
        retentionMillis = retentionHours > 0 ? retentionHours * 3_600_000L : 0L;
        maxRows = config.getLong("max-rows", DEFAULT_MAX_ROWS);
        cleanupIntervalMillis = config.getLong("cleanup-interval-minutes", DEFAULT_CLEANUP_INTERVAL_MINUTES) * 60_000L;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        UserActivityCleanupTask task = new UserActivityCleanupTask(retentionMillis, maxRows);
        KeycloakModelUtils.runJobInTransaction(factory, session -> {
            TimerProvider timer = session.getProvider(TimerProvider.class);
            timer.schedule(new ClusterAwareScheduledTaskRunner(factory, task, cleanupIntervalMillis),
                    cleanupIntervalMillis, CLEANUP_TASK_NAME);
        });
        LOG.infof("Scheduled user-activity outbox cleanup every %d ms (retention %d ms, max rows %d)",
                cleanupIntervalMillis, retentionMillis, maxRows);
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
