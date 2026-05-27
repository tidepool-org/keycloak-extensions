package org.tidepool.keycloak.extensions.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.credential.CredentialModel;
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

import java.util.stream.Stream;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecoveryCodesCleanupEventListenerProviderTest {

    private static final String REALM_ID = "realm-1";
    private static final String USER_ID = "user-1";

    private KeycloakSession session;
    private SubjectCredentialManager credentials;
    private RecoveryCodesCleanupEventListenerProvider provider;

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

        provider = new RecoveryCodesCleanupEventListenerProvider(session);
    }

    private void stubOtp(CredentialModel... models) {
        when(credentials.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE))
                .thenReturn(Stream.of(models));
    }

    private void stubRecoveryCodes(CredentialModel... models) {
        when(credentials.getStoredCredentialsByTypeStream(RecoveryAuthnCodesCredentialModel.TYPE))
                .thenReturn(Stream.of(models));
    }

    private static CredentialModel credential(String id) {
        CredentialModel model = mock(CredentialModel.class);
        lenient().when(model.getId()).thenReturn(id);
        return model;
    }

    private static Event removeTotpEvent() {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(EventType.REMOVE_TOTP);
        lenient().when(event.getRealmId()).thenReturn(REALM_ID);
        lenient().when(event.getUserId()).thenReturn(USER_ID);
        return event;
    }

    @Test
    void deletesRecoveryCodesWhenLastOtpRemoved() {
        stubOtp(); // no OTP left
        stubRecoveryCodes(credential("rc-1"), credential("rc-2"));

        provider.onEvent(removeTotpEvent());

        verify(credentials).removeStoredCredentialById("rc-1");
        verify(credentials).removeStoredCredentialById("rc-2");
    }

    @Test
    void keepsRecoveryCodesWhenOtpStillRemains() {
        stubOtp(credential("otp-1")); // another OTP device remains

        provider.onEvent(removeTotpEvent());

        verify(credentials, never()).removeStoredCredentialById(org.mockito.ArgumentMatchers.anyString());
        // Recovery codes are never even queried when OTP remains.
        verify(credentials, never()).getStoredCredentialsByTypeStream(RecoveryAuthnCodesCredentialModel.TYPE);
    }

    @Test
    void noopWhenNoRecoveryCodesExist() {
        stubOtp(); // no OTP left
        stubRecoveryCodes(); // and no recovery codes to clean up

        provider.onEvent(removeTotpEvent());

        verify(credentials, never()).removeStoredCredentialById(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void ignoresUnrelatedUserEvents() {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(EventType.LOGIN);

        provider.onEvent(event);

        // Never resolves the realm/user for events other than REMOVE_TOTP.
        verify(session, never()).realms();
        verify(session, never()).users();
    }
}
