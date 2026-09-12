package org.tidepool.keycloak.extensions.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.credential.CredentialModel;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;

import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrustedDeviceCleanupEventListenerProviderTest {

    private static final String REALM_ID = "realm-1";
    private static final String USER_ID = "user-1";
    private static final String TRUSTED_DEVICE_TYPE =
            TrustedDeviceCleanupEventListenerProvider.TRUSTED_DEVICE_CREDENTIAL_TYPE;

    private KeycloakSession session;
    private SubjectCredentialManager credentials;
    private TrustedDeviceCleanupEventListenerProvider provider;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        RealmProvider realms = mock(RealmProvider.class);
        UserProvider users = mock(UserProvider.class);
        RealmModel realm = mock(RealmModel.class);
        UserModel user = mock(UserModel.class);
        credentials = mock(SubjectCredentialManager.class);

        lenient().when(session.realms()).thenReturn(realms);
        lenient().when(session.users()).thenReturn(users);
        lenient().when(realms.getRealm(REALM_ID)).thenReturn(realm);
        lenient().when(users.getUserById(realm, USER_ID)).thenReturn(user);
        lenient().when(user.credentialManager()).thenReturn(credentials);
        lenient().when(realm.getName()).thenReturn("test-realm");
        lenient().when(user.getId()).thenReturn(USER_ID);

        provider = new TrustedDeviceCleanupEventListenerProvider(session);
    }

    private void stubTrustedDevices(CredentialModel... models) {
        when(credentials.getStoredCredentialsByTypeStream(TRUSTED_DEVICE_TYPE))
                .thenReturn(Stream.of(models));
    }

    private void stubRemainingOtpDevices(CredentialModel... models) {
        when(credentials.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE))
                .thenReturn(Stream.of(models));
    }

    private static CredentialModel credential(String id) {
        CredentialModel model = mock(CredentialModel.class);
        lenient().when(model.getId()).thenReturn(id);
        return model;
    }

    private static Event event(EventType type, Map<String, String> details) {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(type);
        lenient().when(event.getRealmId()).thenReturn(REALM_ID);
        lenient().when(event.getUserId()).thenReturn(USER_ID);
        lenient().when(event.getDetails()).thenReturn(details);
        return event;
    }

    private static Event updatePasswordEvent() {
        return event(EventType.UPDATE_PASSWORD, null);
    }

    private static Event credentialRemovedEvent(String credentialType) {
        return event(EventType.REMOVE_CREDENTIAL, Map.of(Details.CREDENTIAL_TYPE, credentialType));
    }

    @Test
    void removesAllTrustedDevicesOnPasswordChange() {
        stubTrustedDevices(credential("td-1"), credential("td-2"));

        provider.onEvent(updatePasswordEvent());

        verify(credentials).removeStoredCredentialById("td-1");
        verify(credentials).removeStoredCredentialById("td-2");
    }

    @Test
    void noopWhenNoTrustedDevices() {
        stubTrustedDevices(); // user has no trusted devices

        provider.onEvent(updatePasswordEvent());

        verify(credentials, never()).removeStoredCredentialById(anyString());
    }

    @Test
    void ignoresUnrelatedUserEvents() {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(EventType.LOGIN);

        provider.onEvent(event);

        // Never resolves the realm/user for events other than the handled ones.
        verify(session, never()).realms();
        verify(session, never()).users();
    }

    @Test
    void removesAllTrustedDevicesOnEmailChange() {
        stubTrustedDevices(credential("td-1"));

        provider.onEvent(event(EventType.UPDATE_EMAIL, Map.of(
                Details.PREVIOUS_EMAIL, "old@example.com",
                Details.UPDATED_EMAIL, "new@example.com")));

        verify(credentials).removeStoredCredentialById("td-1");
    }

    @Test
    void removesAllTrustedDevicesOnProfileEmailChange() {
        stubTrustedDevices(credential("td-1"));

        provider.onEvent(event(EventType.UPDATE_PROFILE, Map.of(
                Details.PREVIOUS_EMAIL, "old@example.com",
                Details.UPDATED_EMAIL, "new@example.com")));

        verify(credentials).removeStoredCredentialById("td-1");
    }

    @Test
    void ignoresProfileUpdateWithoutEmailChange() {
        // previous_email / updated_email details are absent when the email did not change.
        provider.onEvent(event(EventType.UPDATE_PROFILE, Map.of("updated_first_name", "Ann")));

        verify(credentials, never()).removeStoredCredentialById(anyString());
    }

    @Test
    void ignoresEmailUpdateWhenAddressOnlyChangedCase() {
        provider.onEvent(event(EventType.UPDATE_EMAIL, Map.of(
                Details.PREVIOUS_EMAIL, "Same@example.com",
                Details.UPDATED_EMAIL, "same@example.com")));

        verify(credentials, never()).removeStoredCredentialById(anyString());
    }

    @Test
    void removesTrustedDevicesWhenLastOtpDeviceRemoved() {
        stubTrustedDevices(credential("td-1"));
        stubRemainingOtpDevices(); // the removed credential was the last OTP device

        provider.onEvent(credentialRemovedEvent(OTPCredentialModel.TYPE));

        verify(credentials).removeStoredCredentialById("td-1");
    }

    @Test
    void keepsTrustedDevicesWhileAnotherOtpDeviceRemains() {
        stubRemainingOtpDevices(credential("otp-2"));

        provider.onEvent(credentialRemovedEvent(OTPCredentialModel.TYPE));

        verify(credentials, never()).removeStoredCredentialById(anyString());
    }

    @Test
    void removesTrustedDevicesWhenRecoveryCodesRemovedAndNoOtpRemains() {
        stubTrustedDevices(credential("td-1"));
        stubRemainingOtpDevices();

        provider.onEvent(credentialRemovedEvent(RecoveryAuthnCodesCredentialModel.TYPE));

        verify(credentials).removeStoredCredentialById("td-1");
    }

    @Test
    void keepsTrustedDevicesWhenRecoveryCodesRemovedButOtpRemains() {
        stubRemainingOtpDevices(credential("otp-1"));

        provider.onEvent(credentialRemovedEvent(RecoveryAuthnCodesCredentialModel.TYPE));

        verify(credentials, never()).removeStoredCredentialById(anyString());
    }

    @Test
    void ignoresRemovalOfNonTwoFactorCredentials() {
        provider.onEvent(credentialRemovedEvent("password"));

        verify(session, never()).realms();
        verify(credentials, never()).removeStoredCredentialById(anyString());
    }

    @Test
    void ignoresLegacyRemoveTotpEvent() {
        // REMOVE_TOTP is fired alongside REMOVE_CREDENTIAL for the same removal; only the
        // latter is handled so the cleanup does not run twice.
        provider.onEvent(event(EventType.REMOVE_TOTP, Map.of(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE)));

        verify(session, never()).realms();
        verify(credentials, never()).removeStoredCredentialById(anyString());
    }
}
