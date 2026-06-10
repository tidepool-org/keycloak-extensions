package org.tidepool.keycloak.extensions.activity;

import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    /** Credential additions/updates: while the user has a second factor, record MFA as enabled. */
    private static final Set<EventType> CREDENTIAL_ADDED_EVENTS = EnumSet.of(
            EventType.UPDATE_CREDENTIAL, EventType.UPDATE_TOTP);

    /** Credential removals: once no second factor remains, record MFA as disabled. */
    private static final Set<EventType> CREDENTIAL_REMOVED_EVENTS = EnumSet.of(
            EventType.REMOVE_CREDENTIAL, EventType.REMOVE_TOTP);

    /** Federated-identity changes after which we re-publish the user's full IdP link set. */
    private static final Set<EventType> IDP_LINK_EVENTS = EnumSet.of(
            EventType.FEDERATED_IDENTITY_LINK, EventType.REMOVE_FEDERATED_IDENTITY,
            EventType.FEDERATED_IDENTITY_OVERRIDE_LINK);

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

    public UserActivityEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        EventType type = event.getType();
        if (type == EventType.LOGIN) {
            recordLogin(event.getRealmId(), event.getUserId(), event.getTime());
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

    private void recordLogin(String realmId, String userId, long time) {
        if (realmId == null || userId == null) {
            return;
        }
        persist(new UserActivityEventEntity(KeycloakModelUtils.generateId(), realmId, userId,
                UserActivityEventEntity.TYPE_LOGIN, null, time));
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
        if (removed) {
            // Disabled only once the removal leaves no second factor (e.g. removing one of two stays enabled).
            if (hasMfa) {
                return;
            }
            record(realmId, userId, time, UserActivityEventEntity.TYPE_MFA_DISABLED);
        } else {
            // Enabled while a second factor is present (a non-MFA add was filtered out above).
            if (!hasMfa) {
                return;
            }
            record(realmId, userId, time, UserActivityEventEntity.TYPE_MFA_ENABLED);
        }
    }

    private void record(String realmId, String userId, long time, String eventType) {
        persist(new UserActivityEventEntity(KeycloakModelUtils.generateId(), realmId, userId,
                eventType, null, time));
        LOG.debugf("Recorded %s for user %s in realm %s", eventType, userId, realmId);
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

        IdentityProviderStorageProvider idps = session.identityProviders();
        List<Map<String, String>> links = session.users().getFederatedIdentitiesStream(realm, user)
                .map(FederatedIdentityModel::getIdentityProvider)
                .sorted()
                .map(alias -> {
                    Map<String, String> link = new LinkedHashMap<>();
                    link.put("alias", alias);
                    link.put("name", displayNameFor(alias, idps.getByAlias(alias)));
                    return link;
                })
                .collect(Collectors.toList());

        String json = toJson(links);
        persist(new UserActivityEventEntity(KeycloakModelUtils.generateId(), realmId, userId,
                UserActivityEventEntity.TYPE_IDP_LINKS_CHANGED, json, time));
        LOG.debugf("Recorded IdP links %s for user %s in realm %s", json, userId, realmId);
    }

    private static String toJson(Object value) {
        try {
            return JsonSerialization.writeValueAsString(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize identity-provider links", e);
        }
    }

    /**
     * The IdP's configured display name, or &mdash; when it has none &mdash; a human-readable name
     * derived from the alias: non-alphanumeric separators become spaces and each word is capitalized
     * (e.g. {@code "google-tidepool"} &rarr; {@code "Google Tidepool"}).
     */
    private static String displayNameFor(String alias, IdentityProviderModel model) {
        if (model != null) {
            String displayName = model.getDisplayName();
            if (displayName != null && !displayName.isBlank()) {
                return displayName;
            }
        }
        return humanize(alias);
    }

    private static String humanize(String alias) {
        StringBuilder humanized = new StringBuilder();
        for (String word : alias.split("[^a-zA-Z0-9]+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (humanized.length() > 0) {
                humanized.append(' ');
            }
            humanized.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        // Fall back to the raw alias if it had nothing alphanumeric to humanize.
        return humanized.length() == 0 ? alias : humanized.toString();
    }

    private RealmModel resolveRealm(String realmId) {
        return realmId == null ? null : session.realms().getRealm(realmId);
    }

    private boolean hasMfaCredential(UserModel user) {
        SubjectCredentialManager credentials = user.credentialManager();
        return MFA_CREDENTIAL_TYPES.stream()
                .anyMatch(type -> credentials.getStoredCredentialsByTypeStream(type).findAny().isPresent());
    }

    private void persist(UserActivityEventEntity entity) {
        entityManager().persist(entity);
    }

    // The EntityManager is owned and closed by the Keycloak session/transaction; we must not close it.
    @SuppressWarnings("resource")
    private EntityManager entityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public void close() {
    }
}
