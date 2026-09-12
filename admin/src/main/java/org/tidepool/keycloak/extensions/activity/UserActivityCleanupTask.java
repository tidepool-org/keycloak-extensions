package org.tidepool.keycloak.extensions.activity;

import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.ScheduledTask;

import java.util.List;

/**
 * Prunes the {@link UserActivityEventEntity} outbox so it cannot grow without bound. Runs on a timer
 * (cluster-aware, so a single node prunes per interval) and applies two bounds:
 *
 * <ol>
 *   <li><b>Age</b> &mdash; deletes rows older than the retention window (default one week); disabled
 *       when {@code retentionMillis <= 0}.</li>
 *   <li><b>Size</b> &mdash; when enabled, trims the oldest rows so at most {@code maxRows} remain.</li>
 * </ol>
 *
 * <p>The size trim is a soft cap: it deletes at or below the boundary timestamp, so ties at the
 * boundary millisecond may leave the table slightly under {@code maxRows}, never over. The boundary is
 * located by scanning only the rows to be dropped (oldest-first, offset by the overflow count), not the
 * whole retained set.
 */
public class UserActivityCleanupTask implements ScheduledTask {

    private static final Logger LOG = Logger.getLogger(UserActivityCleanupTask.class);

    private final long retentionMillis;
    private final long maxRows;

    /**
     * @param retentionMillis age threshold; rows older than this are deleted, or {@code <= 0} to
     *                        disable age-based pruning.
     * @param maxRows         maximum rows to retain, or {@code <= 0} to disable the size cap.
     */
    public UserActivityCleanupTask(long retentionMillis, long maxRows) {
        this.retentionMillis = retentionMillis;
        this.maxRows = maxRows;
    }

    // The EntityManager is owned and closed by the Keycloak session/transaction; we must not close it.
    @SuppressWarnings("resource")
    @Override
    public void run(KeycloakSession session) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();

        int byAge = pruneByAge(em);
        int bySize = trimToMaxRows(em);

        if (byAge + bySize > 0) {
            LOG.infof("Pruned user-activity outbox: %d row(s) by age, %d row(s) by size", byAge, bySize);
        }
    }

    private int pruneByAge(EntityManager em) {
        if (retentionMillis <= 0) {
            return 0; // Age-based pruning disabled.
        }
        long cutoff = Time.currentTimeMillis() - retentionMillis;
        return em.createNamedQuery("TidepoolUserActivityEvent.deleteOlderThan")
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }

    private int trimToMaxRows(EntityManager em) {
        if (maxRows <= 0) {
            return 0;
        }

        long count = em.createNamedQuery("TidepoolUserActivityEvent.count", Long.class).getSingleResult();
        long excess = count - maxRows;
        if (excess <= 0) {
            return 0;
        }

        // Boundary = the newest event time among the `excess` oldest rows. Scanning oldest-first and
        // offsetting by (excess - 1) touches only the rows we intend to drop, not all maxRows kept.
        List<Long> boundary = em.createNamedQuery("TidepoolUserActivityEvent.eventTimesAsc", Long.class)
                .setFirstResult((int) Math.min(excess - 1, Integer.MAX_VALUE))
                .setMaxResults(1)
                .getResultList();
        if (boundary.isEmpty()) {
            return 0;
        }

        return em.createNamedQuery("TidepoolUserActivityEvent.deleteAtOrOlderThan")
                .setParameter("threshold", boundary.get(0))
                .executeUpdate();
    }

    @Override
    public String getTaskName() {
        return UserActivityEventListenerProviderFactory.CLEANUP_TASK_NAME;
    }
}
