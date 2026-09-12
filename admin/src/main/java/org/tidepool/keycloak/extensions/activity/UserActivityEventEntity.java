package org.tidepool.keycloak.extensions.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

/**
 * Outbox row recording a user-activity change worth propagating to an external system via CDC.
 *
 * <p>This is a purpose-built outbox table rather than Keycloak's generic {@code EVENT_ENTITY} store:
 * it carries a flat, stable schema for the CDC connector, holds <em>only</em> the handful of facts we
 * want to publish, and is owned (and pruned) by this extension. Rows are written in the same
 * transaction as the Keycloak action that produced them (the transactional-outbox pattern), and are
 * pruned by {@link UserActivityCleanupTask}.
 *
 * <p>Each row is one of:
 * <ul>
 *   <li>{@code LOGIN} &mdash; a successful browser login; {@link #eventTime} is the login time.</li>
 *   <li>{@code MFA_ENABLED} / {@code MFA_DISABLED} &mdash; the user gained, or no longer has, a
 *       two-factor credential. The {@link #eventType} is the state; there is no separate flag.</li>
 *   <li>{@code IDP_LINKS_CHANGED} &mdash; an identity-provider link was added or removed;
 *       {@link #identityProviders} carries the user's full set of linked IdPs afterwards, as JSON.</li>
 * </ul>
 */
@Entity
@Table(name = "TIDEPOOL_USER_ACTIVITY_EVENT")
@NamedQueries({
        // Time-based pruning: drop everything strictly older than the retention cutoff.
        @NamedQuery(name = "TidepoolUserActivityEvent.deleteOlderThan",
                query = "DELETE FROM UserActivityEventEntity e WHERE e.eventTime < :cutoff"),
        // Size-based pruning: drop everything at or older than a boundary timestamp.
        @NamedQuery(name = "TidepoolUserActivityEvent.deleteAtOrOlderThan",
                query = "DELETE FROM UserActivityEventEntity e WHERE e.eventTime <= :threshold"),
        @NamedQuery(name = "TidepoolUserActivityEvent.count",
                query = "SELECT COUNT(e) FROM UserActivityEventEntity e"),
        // Event times oldest-first; used with setFirstResult(excess - 1) to find the size-based boundary
        // by scanning only the rows to be dropped, not the whole retained set.
        @NamedQuery(name = "TidepoolUserActivityEvent.eventTimesAsc",
                query = "SELECT e.eventTime FROM UserActivityEventEntity e ORDER BY e.eventTime ASC")
})
public class UserActivityEventEntity {

    public static final String TYPE_LOGIN = "LOGIN";
    public static final String TYPE_MFA_ENABLED = "MFA_ENABLED";
    public static final String TYPE_MFA_DISABLED = "MFA_DISABLED";
    public static final String TYPE_IDP_LINKS_CHANGED = "IDP_LINKS_CHANGED";

    @Id
    @Column(name = "ID", length = 36)
    private String id;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Column(name = "USER_ID", nullable = false)
    private String userId;

    @Column(name = "EVENT_TYPE", nullable = false, length = 32)
    private String eventType;

    /**
     * For {@code IDP_LINKS_CHANGED} rows, a JSON array of the IdPs the user is linked to, sorted by
     * alias, e.g. {@code [{"alias":"google","name":"Google"}]} (where {@code name} is the provider's
     * display name, or a humanized alias when it has none); an empty array {@code []} when the last
     * link was removed. {@code null} for other event types.
     *
     * <p>Sized at the portable {@code VARCHAR} maximum (4000) so realistic link sets never overflow:
     * because the row is written in the source transaction, a length violation would roll back the
     * triggering action, not just the outbox write.
     */
    @Column(name = "IDENTITY_PROVIDERS", length = 4000)
    private String identityProviders;

    /** Epoch milliseconds the source event occurred; drives both ordering and pruning. */
    @Column(name = "EVENT_TIME", nullable = false)
    private long eventTime;

    public UserActivityEventEntity() {
    }

    public UserActivityEventEntity(String id, String realmId, String userId, String eventType,
                                   String identityProviders, long eventTime) {
        this.id = id;
        this.realmId = realmId;
        this.userId = userId;
        this.eventType = eventType;
        this.identityProviders = identityProviders;
        this.eventTime = eventTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getIdentityProviders() {
        return identityProviders;
    }

    public void setIdentityProviders(String identityProviders) {
        this.identityProviders = identityProviders;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }
}
