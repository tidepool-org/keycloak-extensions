package org.tidepool.keycloak.extensions.activity;

import jakarta.persistence.EntityManager;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Writes {@link UserActivityEventEntity} rows. Shared by the live {@link UserActivityEventListenerProvider}
 * and the one-off backfill endpoint so both produce byte-for-byte identical rows. Persists through the
 * session's shared JPA connection (the transactional-outbox pattern).
 */
public class UserActivityRecorder {

    private final KeycloakSession session;

    public UserActivityRecorder(KeycloakSession session) {
        this.session = session;
    }

    /** Persist a {@code LOGIN} row. */
    public void recordLogin(String realmId, String userId, long time) {
        persist(new UserActivityEventEntity(KeycloakModelUtils.generateId(), realmId, userId,
                UserActivityEventEntity.TYPE_LOGIN, null, time));
    }

    /** Persist an {@code MFA_ENABLED} or {@code MFA_DISABLED} row reflecting the user's new MFA state. */
    public void recordMfa(String realmId, String userId, boolean mfaEnabled, long time) {
        persist(new UserActivityEventEntity(KeycloakModelUtils.generateId(), realmId, userId,
                mfaEnabled ? UserActivityEventEntity.TYPE_MFA_ENABLED : UserActivityEventEntity.TYPE_MFA_DISABLED,
                null, time));
    }

    /** Persist an {@code IDP_LINKS_CHANGED} row capturing the user's current set of linked IdPs. */
    public void recordIdpLinks(RealmModel realm, UserModel user, long time) {
        persistIdpLinks(realm, user, identityProvidersJson(realm, user), time);
    }

    /**
     * Like {@link #recordIdpLinks} but skips users with no current links (used by the backfill, which
     * seeds <em>existing</em> links and should not emit empty {@code []} rows).
     *
     * @return whether a row was recorded.
     */
    public boolean recordIdpLinksIfPresent(RealmModel realm, UserModel user, long time) {
        String json = identityProvidersJson(realm, user);
        if ("[]".equals(json)) {
            return false;
        }
        persistIdpLinks(realm, user, json, time);
        return true;
    }

    private void persistIdpLinks(RealmModel realm, UserModel user, String json, long time) {
        persist(new UserActivityEventEntity(KeycloakModelUtils.generateId(), realm.getId(), user.getId(),
                UserActivityEventEntity.TYPE_IDP_LINKS_CHANGED, json, time));
    }

    private void persist(UserActivityEventEntity entity) {
        entityManager().persist(entity);
    }

    /** JSON array of the user's linked IdPs, sorted by alias: {@code [{"alias":"google","name":"Google"}]}. */
    private String identityProvidersJson(RealmModel realm, UserModel user) {
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
        return toJson(links);
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
    static String displayNameFor(String alias, IdentityProviderModel model) {
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
        return humanized.length() == 0 ? alias : humanized.toString();
    }

    // The EntityManager is owned and closed by the Keycloak session/transaction; we must not close it.
    @SuppressWarnings("resource")
    private EntityManager entityManager() {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }
}
