package org.tidepool.keycloak.extensions.events;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Removes all of a user's trusted devices when the account's security posture changes.
 *
 * <p>A trusted device lets a user skip two-factor authentication on a previously verified device.
 * Trust established under the old account state should not carry over, so every stored
 * {@value #TRUSTED_DEVICE_CREDENTIAL_TYPE} credential (how the trusted-device SPI persists trust)
 * is deleted when any of the following user events fire:
 *
 * <ul>
 *   <li>{@link EventType#UPDATE_PASSWORD} &mdash; the self-service "update password" action and the
 *   reset-credentials (forgot-password) flow.</li>
 *   <li>{@link EventType#UPDATE_EMAIL} / {@link EventType#UPDATE_PROFILE} &mdash; but only when the
 *   email actually changed. Both events carry {@link Details#PREVIOUS_EMAIL} /
 *   {@link Details#UPDATED_EMAIL} only in that case, so a present previous address is the change
 *   signal (the same detection {@link EmailChangedNotificationEventListenerProvider} uses).</li>
 *   <li>{@link EventType#REMOVE_CREDENTIAL} of an {@code otp} or {@code recovery-authn-codes}
 *   credential, once no OTP credential remains &mdash; i.e. the user disabled two-factor
 *   authentication. Without this, a device trusted before (or during) the removal would let the
 *   user skip the OTP prompt after re-enrolling. The remaining-OTP check mirrors
 *   {@link RecoveryCodesCleanupEventListenerProvider}: removing one of several OTP devices keeps
 *   the trust. The legacy {@link EventType#REMOVE_TOTP} twin that Keycloak fires alongside is
 *   deliberately ignored to avoid handling the same removal twice.</li>
 * </ul>
 *
 * <p>Admin-initiated changes (password resets, credential deletions, email edits) emit only admin
 * events and are intentionally out of scope.
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
        switch (event.getType()) {
            case UPDATE_PASSWORD:
                removeTrustedDevices(event, "a password change");
                break;
            case UPDATE_EMAIL:
            case UPDATE_PROFILE:
                if (emailChanged(event)) {
                    removeTrustedDevices(event, "an email change");
                }
                break;
            case REMOVE_CREDENTIAL:
                onCredentialRemoved(event);
                break;
            default:
                break;
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Self-service changes only; admin-initiated password resets, credential deletions and
        // email edits are out of scope.
    }

    private static boolean emailChanged(Event event) {
        Map<String, String> details = event.getDetails();
        if (details == null) {
            return false;
        }
        String previousEmail = details.get(Details.PREVIOUS_EMAIL);
        String updatedEmail = details.get(Details.UPDATED_EMAIL);
        if (previousEmail == null || previousEmail.isBlank()) {
            return false;
        }
        return updatedEmail == null || !previousEmail.equalsIgnoreCase(updatedEmail);
    }

    private void onCredentialRemoved(Event event) {
        Map<String, String> details = event.getDetails();
        String credentialType = details == null ? null : details.get(Details.CREDENTIAL_TYPE);
        if (!OTPCredentialModel.TYPE.equals(credentialType)
                && !RecoveryAuthnCodesCredentialModel.TYPE.equals(credentialType)) {
            return;
        }

        RealmModel realm = resolveRealm(event);
        UserModel user = resolveUser(realm, event);
        if (user == null) {
            return;
        }

        // The event fires after the credential is gone from the store. Keep the trust while any
        // OTP device remains; recovery codes alone don't count because they are only issued as an
        // OTP backup and are cascade-removed with the last OTP device.
        boolean hasOtp = user.credentialManager()
                .getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE)
                .findAny()
                .isPresent();
        if (hasOtp) {
            return;
        }

        removeTrustedDevices(realm, user, "two-factor authentication was disabled");
    }

    private void removeTrustedDevices(Event event, String reason) {
        RealmModel realm = resolveRealm(event);
        UserModel user = resolveUser(realm, event);
        if (user == null) {
            return;
        }
        removeTrustedDevices(realm, user, reason);
    }

    private RealmModel resolveRealm(Event event) {
        if (event.getRealmId() == null) {
            return null;
        }
        return session.realms().getRealm(event.getRealmId());
    }

    private UserModel resolveUser(RealmModel realm, Event event) {
        if (realm == null || event.getUserId() == null) {
            return null;
        }
        return session.users().getUserById(realm, event.getUserId());
    }

    private static void removeTrustedDevices(RealmModel realm, UserModel user, String reason) {
        SubjectCredentialManager credentials = user.credentialManager();
        List<String> trustedDeviceIds = credentials
                .getStoredCredentialsByTypeStream(TRUSTED_DEVICE_CREDENTIAL_TYPE)
                .map(CredentialModel::getId)
                .collect(Collectors.toList());

        if (trustedDeviceIds.isEmpty()) {
            return;
        }

        trustedDeviceIds.forEach(credentials::removeStoredCredentialById);
        LOG.infof("Removed %d trusted device(s) for user %s in realm %s after %s",
                trustedDeviceIds.size(), user.getId(), realm.getName(), reason);
    }

    @Override
    public void close() {
    }
}
