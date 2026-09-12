package org.tidepool.keycloak.extensions.activity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserActivityCleanupTaskTest {

    private static final long RETENTION_MILLIS = 7L * 24 * 3_600_000L;

    private KeycloakSession session;
    private EntityManager em;
    private Query deleteOlderThan;
    private Query deleteAtOrOlderThan;
    private TypedQuery<Long> countQuery;
    private TypedQuery<Long> eventTimesQuery;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        session = mock(KeycloakSession.class);
        em = mock(EntityManager.class);
        deleteOlderThan = mock(Query.class);
        deleteAtOrOlderThan = mock(Query.class);
        countQuery = mock(TypedQuery.class);
        eventTimesQuery = mock(TypedQuery.class);

        JpaConnectionProvider jpa = mock(JpaConnectionProvider.class);
        lenient().when(session.getProvider(JpaConnectionProvider.class)).thenReturn(jpa);
        lenient().when(jpa.getEntityManager()).thenReturn(em);

        lenient().when(em.createNamedQuery("TidepoolUserActivityEvent.deleteOlderThan")).thenReturn(deleteOlderThan);
        lenient().when(deleteOlderThan.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(deleteOlderThan);
        lenient().when(deleteOlderThan.executeUpdate()).thenReturn(3);

        lenient().when(em.createNamedQuery("TidepoolUserActivityEvent.deleteAtOrOlderThan")).thenReturn(deleteAtOrOlderThan);
        lenient().when(deleteAtOrOlderThan.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(deleteAtOrOlderThan);
        lenient().when(deleteAtOrOlderThan.executeUpdate()).thenReturn(5);

        lenient().when(em.createNamedQuery("TidepoolUserActivityEvent.count", Long.class)).thenReturn(countQuery);

        lenient().when(em.createNamedQuery("TidepoolUserActivityEvent.eventTimesAsc", Long.class)).thenReturn(eventTimesQuery);
        lenient().when(eventTimesQuery.setFirstResult(anyInt())).thenReturn(eventTimesQuery);
        lenient().when(eventTimesQuery.setMaxResults(anyInt())).thenReturn(eventTimesQuery);
    }

    @Test
    void deletesByAgeAndSkipsSizeTrimWhenUnderCap() {
        when(countQuery.getSingleResult()).thenReturn(100L);

        new UserActivityCleanupTask(RETENTION_MILLIS, 1000L).run(session);

        verify(deleteOlderThan).executeUpdate();
        verify(deleteAtOrOlderThan, never()).executeUpdate();
    }

    @Test
    void trimsBySizeWhenOverCap() {
        when(countQuery.getSingleResult()).thenReturn(1500L); // excess = 500 oldest rows to drop
        when(eventTimesQuery.getResultList()).thenReturn(List.of(1_700_000_000_000L));

        new UserActivityCleanupTask(RETENTION_MILLIS, 1000L).run(session);

        // Boundary located by scanning only the overflow (offset excess-1 = 499), oldest-first.
        verify(eventTimesQuery).setFirstResult(499);
        verify(deleteAtOrOlderThan).setParameter(eq("threshold"), eq(1_700_000_000_000L));
        verify(deleteAtOrOlderThan).executeUpdate();
    }

    @Test
    void skipsSizeTrimWhenDisabled() {
        new UserActivityCleanupTask(RETENTION_MILLIS, 0L).run(session);

        verify(deleteOlderThan).executeUpdate();
        verify(em, never()).createNamedQuery("TidepoolUserActivityEvent.count", Long.class);
        verify(deleteAtOrOlderThan, never()).executeUpdate();
    }

    @Test
    void skipsAgePruneWhenRetentionDisabled() {
        when(countQuery.getSingleResult()).thenReturn(100L);

        new UserActivityCleanupTask(0L, 1000L).run(session); // retention disabled, size cap active

        verify(deleteOlderThan, never()).executeUpdate();
    }
}
