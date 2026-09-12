package org.tidepool.keycloak.extensions.activity;

import org.jboss.logging.Logger;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;

import java.util.EnumSet;
import java.util.Set;

/**
 * Writes a compact stream of user-activity facts into the {@link UserActivityEventEntity} outbox so an
 * external system can consume them via CDC. It records exactly three things, and nothing else:
 *
 * <ul>
 *   <li><b>Last login</b> &mdash; one row per successful {@link EventType#LOGIN}.</li>
 *   <li><b>MFA enabled/disabled</b> &mdash; the direction comes from the event itself, not from stored
 *       history (the outbox is pruned, so it cannot be its own source of truth). Adding/updating a
 *       second-factor credential records {@code MFA_ENABLED} while the user has one; removing a
 *       second-factor credential records {@code MFA_DISABLED} only once none remain. Either row always
 *       states the user's true current state, so it is safe for the consumer to upsert and ignore
 *       repeats. Non-second-factor credential changes (e.g. passwords) produce no row.</li>
 *   <li><b>IdP links changed</b> &mdash; on a federated-identity link or unlink, it records the user's
 *       full current set of linked identity-provider aliases.</li>
 * </ul>
 *
 * <p>Rows are persisted through the request's shared JPA session, so they commit atomically with the
 * Keycloak action that triggered them (the transactional-outbox pattern).
 *
 * <p>Both self-service (user) and admin-initiated changes are captured: the latter arrive as
 * {@link AdminEvent}s whose {@link AdminEvent#getResourcePath() resource path} identifies the affected
 * user and the kind of change. Login is inherently a user event and has no admin equivalent.
 */
public class UserActivityEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(UserActivityEventListenerProvider.class);

    /**
     * Credential additions/updates: while the user has a second factor, record MFA as enabled.
     *
     * <p>Deliberately excludes the deprecated {@link EventType#UPDATE_TOTP}/{@link EventType#REMOVE_TOTP}:
     * Keycloak fires those as <em>extra duplicates alongside</em> the consolidated
     * {@code UPDATE_CREDENTIAL}/{@code REMOVE_CREDENTIAL} events on every OTP change (it clones the
     * event builder and emits both), so listening to both records every OTP change twice.
     */
    private static final Set<EventType> CREDENTIAL_ADDED_EVENTS = EnumSet.of(EventType.UPDATE_CREDENTIAL);

    /** Credential removals: once no second factor remains, record MFA as disabled. See note above. */
    private static final Set<EventType> CREDENTIAL_REMOVED_EVENTS = EnumSet.of(EventType.REMOVE_CREDENTIAL);

    /** Federated-identity changes after which we re-publish the user's full IdP link set. */
    private static final Set<EventType> IDP_LINK_EVENTS = EnumSet.of(
            EventType.FEDERATED_IDENTITY_LINK, EventType.REMOVE_FEDERATED_IDENTITY,
            EventType.FEDERATED_IDENTITY_OVERRIDE_LINK);

    /**
     * Note set on a {@code UserSession} once a LOGIN row has been recorded for it. Keycloak fires a
     * LOGIN event for every client the user reaches, including silent cookie/SSO re-authentications
     * that reuse the session; the note makes the dedup deterministic — exactly one LOGIN row per
     * session — without any time-window heuristic (which would mis-handle slow required actions,
     * SSO bursts, and clock skew).
     */
    static final String LOGIN_RECORDED_NOTE = "tidepool-user-activity-login-recorded";

    /**
     * User profile attribute updated alongside each recorded LOGIN row. The outbox is pruned, so the
     * attribute is the durable "last login" record; external systems read it through the user model
     * (e.g. shoreline's get-user endpoint) to backfill state for users with no recent outbox rows.
     * Epoch milliseconds, same clock and format as the outbox {@code EVENT_TIME}.
     */
    static final String LAST_LOGIN_TIME_ATTRIBUTE = "last_login_time";

    /** Credential types that count as a second factor for the MFA-enabled determination. */
    private static final Set<String> MFA_CREDENTIAL_TYPES = Set.of(
            OTPCredentialModel.TYPE,
            WebAuthnCredentialModel.TYPE_TWOFACTOR,
            WebAuthnCredentialModel.TYPE_PASSWORDLESS);

    // Admin resource-path categories ({@code users/{id}/<category>/...}).
    private static final String FEDERATED_IDENTITY_PATH_SEGMENT = "federated-identity";
    private static final String CREDENTIALS_PATH_SEGMENT = "credentials";
    private static final String DISABLE_CREDENTIAL_TYPES_PATH_SEGMENT = "disable-credential-types";

    private final KeycloakSession session;
    private final UserActivityRecorder recorder;

    public UserActivityEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.recorder = new UserActivityRecorder(session);
    }

    @Override
    public void onEvent(Event event) {
        EventType type = event.getType();
        if (type == EventType.LOGIN) {
            recordLogin(event.getRealmId(), event.getUserId(), event.getSessionId(), event.getTime());
        } else if (CREDENTIAL_ADDED_EVENTS.contains(type)) {
            recordMfaCredentialChange(event.getRealmId(), event.getUserId(), event.getTime(),
                    credentialType(event), false);
        } else if (CREDENTIAL_REMOVED_EVENTS.contains(type)) {
            recordMfaCredentialChange(event.getRealmId(), event.getUserId(), event.getTime(),
                    credentialType(event), true);
        } else if (IDP_LINK_EVENTS.contains(type)) {
            recordIdpLinks(event.getRealmId(), event.getUserId(), event.getTime());
        }
    }

    private static String credentialType(Event event) {
        return event.getDetails() == null ? null : event.getDetails().get(Details.CREDENTIAL_TYPE);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (event.getError() != null) {
            return; // Only successful operations.
        }

        // Resource path mirrors the admin REST URI: users/{userId}/{category}/...
        String path = event.getResourcePath();
        if (path == null) {
            return;
        }
        String[] segments = path.split("/");
        if (segments.length < 3 || !"users".equals(segments[0])) {
            return;
        }

        String realmId = event.getRealmId(); // The administered realm (where the user lives).
        String userId = segments[1];
        String category = segments[2];
        if (FEDERATED_IDENTITY_PATH_SEGMENT.equals(category)) {
            recordIdpLinks(realmId, userId, event.getTime());
        } else if (DISABLE_CREDENTIAL_TYPES_PATH_SEGMENT.equals(category)) {
            // Admin bulk-removes credentials of a type. The path carries no credential type, so the
            // recompute below decides whether any second factor remains.
            recordMfaCredentialChange(realmId, userId, event.getTime(), null, true);
        } else if (CREDENTIALS_PATH_SEGMENT.equals(category) && event.getOperationType() == OperationType.DELETE) {
            // A specific credential was deleted by an admin (relabel/reorder are not deletions).
            recordMfaCredentialChange(realmId, userId, event.getTime(), null, true);
        }
        // Admins cannot enrol a second factor for a user, so there is no admin "credential added" path.
    }

    private void recordLogin(String realmId, String userId, String sessionId, long time) {
        if (realmId == null || userId == null) {
            return;
        }
        // Record exactly one LOGIN per user session: the first LOGIN event marks the session, and the
        // cookie/SSO re-logins that follow (one per additional client) see the mark and are skipped.
        // Whenever the session cannot be resolved, fail toward recording rather than losing a login.
        UserSessionModel userSession = lookupUserSession(realmId, sessionId);
        if (userSession != null && userSession.getNote(LOGIN_RECORDED_NOTE) != null) {
            return; // Already recorded for this session — a cookie/SSO re-login for another client.
        }
        recorder.recordLogin(realmId, userId, time);
        updateLastLoginAttribute(realmId, userId, time);
        if (userSession != null) {
            userSession.setNote(LOGIN_RECORDED_NOTE, "true");
        }
    }

    /**
     * Mirrors the recorded login time onto the user's {@link #LAST_LOGIN_TIME_ATTRIBUTE} profile
     * attribute, committing in the same transaction as the outbox row. Skipped silently when the
     * realm or user cannot be resolved — the outbox row is still recorded.
     */
    private void updateLastLoginAttribute(String realmId, String userId, long time) {
        RealmModel realm = resolveRealm(realmId);
        if (realm == null) {
            return;
        }
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            return;
        }
        user.setSingleAttribute(LAST_LOGIN_TIME_ATTRIBUTE, Long.toString(time));
    }

    private UserSessionModel lookupUserSession(String realmId, String sessionId) {
        if (sessionId == null) {
            return null;
        }
        RealmModel realm = resolveRealm(realmId);
        if (realm == null) {
            return null;
        }
        return session.sessions().getUserSession(realm, sessionId);
    }

    /**
     * Records an MFA enable/disable row based on the kind of credential change, without consulting any
     * stored history. {@code removed} marks a credential removal (→ possibly {@code MFA_DISABLED});
     * otherwise it is an add/update (→ possibly {@code MFA_ENABLED}). The recorded row always reflects
     * the user's true current MFA state.
     *
     * @param credentialType the changed credential's type if known, else {@code null}.
     */
    private void recordMfaCredentialChange(String realmId, String userId, long time,
                                           String credentialType, boolean removed) {
        // If we know which credential changed and it is not a second factor, MFA state cannot have
        // changed — skip the user lookup and credential scan entirely (e.g. password changes).
        if (credentialType != null && !MFA_CREDENTIAL_TYPES.contains(credentialType)) {
            return;
        }

        RealmModel realm = resolveRealm(realmId);
        if (realm == null) {
            return;
        }
        UserModel user = userId == null ? null : session.users().getUserById(realm, userId);
        if (user == null) {
            return;
        }

        boolean hasMfa = hasMfaCredential(user);
        // Removal counts as disabled only once no second factor remains (removing one of two stays
        // enabled); an add/update counts as enabled while a second factor is present.
        if (removed == hasMfa) {
            return;
        }
        recorder.recordMfa(realmId, userId, hasMfa, time);
        LOG.debugf("Recorded MFA %s for user %s in realm %s", hasMfa ? "enabled" : "disabled", userId, realmId);
    }

    private void recordIdpLinks(String realmId, String userId, long time) {
        RealmModel realm = resolveRealm(realmId);
        if (realm == null) {
            return;
        }
        UserModel user = userId == null ? null : session.users().getUserById(realm, userId);
        if (user == null) {
            return;
        }
        recorder.recordIdpLinks(realm, user, time);
        LOG.debugf("Recorded IdP links for user %s in realm %s", userId, realmId);
    }

    private RealmModel resolveRealm(String realmId) {
        return realmId == null ? null : session.realms().getRealm(realmId);
    }

    private boolean hasMfaCredential(UserModel user) {
        SubjectCredentialManager credentials = user.credentialManager();
        return MFA_CREDENTIAL_TYPES.stream()
                .anyMatch(type -> credentials.getStoredCredentialsByTypeStream(type).findAny().isPresent());
    }

    @Override
    public void close() {
    }
}
