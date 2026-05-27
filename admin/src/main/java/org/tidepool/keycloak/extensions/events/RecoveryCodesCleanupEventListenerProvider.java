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
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Deletes a user's recovery authentication codes once their last OTP device is removed.
 *
 * <p>Tidepool issues recovery codes only as a backup for OTP-based two-factor authentication.
 * Leaving them behind after every OTP credential is gone would let a user keep a second factor
 * they can no longer use to enrol an authenticator, so we clean them up.
 *
 * <p>When a user removes an OTP device via self-service (the account console or an app-initiated
 * action), Keycloak fires {@link EventType#REMOVE_TOTP} <em>after</em> the credential has already
 * been deleted from the store. At that point we count the remaining OTP credentials; if none
 * remain, the recovery codes are removed too. Admin-initiated credential removals are intentionally
 * out of scope (the admin event carries no credential type), so this only reacts to user events.
 */
public class RecoveryCodesCleanupEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(RecoveryCodesCleanupEventListenerProvider.class);

    private final KeycloakSession session;

    public RecoveryCodesCleanupEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() != EventType.REMOVE_TOTP) {
            return;
        }
        deleteRecoveryCodesIfNoOtpRemains(event.getRealmId(), event.getUserId());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Self-service OTP removals only; admin-initiated credential removals are out of scope.
    }

    private void deleteRecoveryCodesIfNoOtpRemains(String realmId, String userId) {
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
        boolean hasOtp = credentials.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE).findAny().isPresent();
        if (hasOtp) {
            return;
        }

        List<String> recoveryCodeIds = credentials
                .getStoredCredentialsByTypeStream(RecoveryAuthnCodesCredentialModel.TYPE)
                .map(CredentialModel::getId)
                .collect(Collectors.toList());

        if (recoveryCodeIds.isEmpty()) {
            return;
        }

        recoveryCodeIds.forEach(credentials::removeStoredCredentialById);
        LOG.infof("Removed %d recovery-code credential(s) for user %s in realm %s after the last OTP device was deleted",
                recoveryCodeIds.size(), userId, realm.getName());
    }

    @Override
    public void close() {
    }
}
