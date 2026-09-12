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

        provider = new TrustedDeviceCleanupEventListenerProvider(session);
    }

    private void stubTrustedDevices(CredentialModel... models) {
        when(credentials.getStoredCredentialsByTypeStream(TRUSTED_DEVICE_TYPE))
                .thenReturn(Stream.of(models));
    }

    private static CredentialModel credential(String id) {
        CredentialModel model = mock(CredentialModel.class);
        lenient().when(model.getId()).thenReturn(id);
        return model;
    }

    private static Event updatePasswordEvent() {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(EventType.UPDATE_PASSWORD);
        lenient().when(event.getRealmId()).thenReturn(REALM_ID);
        lenient().when(event.getUserId()).thenReturn(USER_ID);
        return event;
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

        // Never resolves the realm/user for events other than UPDATE_PASSWORD.
        verify(session, never()).realms();
        verify(session, never()).users();
    }
}
