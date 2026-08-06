package org.tidepool.keycloak.extensions.events;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Removes all of a user's trusted devices when their password changes.
 *
 * <p>A trusted device lets a user skip two-factor authentication on a previously verified device.
 * When the password changes &mdash; whether the user updates it themselves or completes a
 * forgot-password reset &mdash; any existing trust should be revoked, so trust established under
 * the old password does not carry over to the new one.
 *
 * <p>Keycloak fires {@link EventType#UPDATE_PASSWORD} after the password has been set, for both the
 * self-service "update password" action and the reset-credentials (forgot-password) flow. We react
 * by deleting every stored {@value #TRUSTED_DEVICE_CREDENTIAL_TYPE} credential for the user, which
 * is how the trusted-device SPI persists trust. Admin-initiated password resets emit only an admin
 * event and are intentionally out of scope.
 */
public class TrustedDeviceCleanupEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(TrustedDeviceCleanupEventListenerProvider.class);

    /**
     * Credential type persisted by the trusted-device SPI
     * ({@code nl.wouterh.keycloak.trusteddevice.credential.TrustedDeviceCredentialModel.TYPE_TWOFACTOR}).
     * Hard-coded so this module need not depend on the trusted-device submodule; the value is a
     * stored credential type and is therefore a stable contract.
     */
    static final String TRUSTED_DEVICE_CREDENTIAL_TYPE = "trusted-device";

    private final KeycloakSession session;

    public TrustedDeviceCleanupEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.UPDATE_PASSWORD) {
            return;
        }
        removeTrustedDevices(event.getRealmId(), event.getUserId());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Self-service / reset-credentials password changes only; admin password resets are out of scope.
    }

    private void removeTrustedDevices(String realmId, String userId) {
        if (realmId == null || userId == null) {
            return;
        }

        RealmModel realm = session.realms().getRealm(realmId);
        if (realm == null) {
            return;
        }

        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            return;
        }

        SubjectCredentialManager credentials = user.credentialManager();
        List<String> trustedDeviceIds = credentials
                .getStoredCredentialsByTypeStream(TRUSTED_DEVICE_CREDENTIAL_TYPE)
                .map(CredentialModel::getId)
                .collect(Collectors.toList());

        if (trustedDeviceIds.isEmpty()) {
            return;
        }

        trustedDeviceIds.forEach(credentials::removeStoredCredentialById);
        LOG.infof("Removed %d trusted device(s) for user %s in realm %s after a password change",
                trustedDeviceIds.size(), userId, realm.getName());
    }

    @Override
    public void close() {
    }
}
