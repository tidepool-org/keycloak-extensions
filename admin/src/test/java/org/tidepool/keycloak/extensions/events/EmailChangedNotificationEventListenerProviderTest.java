package org.tidepool.keycloak.extensions.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailChangedNotificationEventListenerProviderTest {

    private static final String REALM_ID = "realm-1";
    private static final String USER_ID = "user-1";
    private static final String OLD_EMAIL = "old@example.com";
    private static final String NEW_EMAIL = "new@example.com";

    private KeycloakSession session;
    private UserModel user;
    private EmailTemplateProvider emailTemplateProvider;
    private EmailChangedNotificationEventListenerProvider provider;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        RealmProvider realms = mock(RealmProvider.class);
        UserProvider users = mock(UserProvider.class);
        RealmModel realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        emailTemplateProvider = mock(EmailTemplateProvider.class);

        lenient().when(session.realms()).thenReturn(realms);
        lenient().when(session.users()).thenReturn(users);
        lenient().when(realms.getRealm(REALM_ID)).thenReturn(realm);
        lenient().when(users.getUserById(realm, USER_ID)).thenReturn(user);
        lenient().when(realm.getName()).thenReturn("test-realm");
        // The user's email is already the NEW address by the time the event fires.
        lenient().when(user.getEmail()).thenReturn(NEW_EMAIL);

        lenient().when(session.getProvider(EmailTemplateProvider.class)).thenReturn(emailTemplateProvider);
        lenient().when(emailTemplateProvider.setRealm(any())).thenReturn(emailTemplateProvider);
        lenient().when(emailTemplateProvider.setUser(any(UserModel.class))).thenReturn(emailTemplateProvider);

        provider = new EmailChangedNotificationEventListenerProvider(session);
    }

    private Event emailChangeEvent(EventType type, String previousEmail, String updatedEmail) {
        Map<String, String> details = new HashMap<>();
        if (previousEmail != null) {
            details.put(Details.PREVIOUS_EMAIL, previousEmail);
        }
        if (updatedEmail != null) {
            details.put(Details.UPDATED_EMAIL, updatedEmail);
        }
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(type);
        lenient().when(event.getDetails()).thenReturn(details);
        lenient().when(event.getRealmId()).thenReturn(REALM_ID);
        lenient().when(event.getUserId()).thenReturn(USER_ID);
        lenient().when(event.getTime()).thenReturn(1_700_000_000_000L);
        lenient().when(event.getIpAddress()).thenReturn("203.0.113.7");
        return event;
    }

    @Test
    void notifiesPreviousAddressOnUpdateEmail() throws Exception {
        provider.onEvent(emailChangeEvent(EventType.UPDATE_EMAIL, OLD_EMAIL, NEW_EMAIL));

        // Delivery is redirected to the previous address, not the user's (new) email.
        ArgumentCaptor<UserModel> recipient = ArgumentCaptor.forClass(UserModel.class);
        verify(emailTemplateProvider).setUser(recipient.capture());
        assertThat(recipient.getValue().getEmail()).isEqualTo(OLD_EMAIL);

        ArgumentCaptor<Map<String, Object>> attrs = ArgumentCaptor.forClass(Map.class);
        verify(emailTemplateProvider).send(
                eq(EmailChangedNotificationEventListenerProvider.SUBJECT_MESSAGE_KEY),
                eq(EmailChangedNotificationEventListenerProvider.TEMPLATE_NAME),
                attrs.capture());
        assertThat(attrs.getValue())
                .containsEntry("newEmail", NEW_EMAIL)
                .containsEntry("ipAddress", "203.0.113.7")
                .containsKey("changedAt");
    }

    @Test
    void notifiesPreviousAddressOnLegacyUpdateProfile() throws Exception {
        provider.onEvent(emailChangeEvent(EventType.UPDATE_PROFILE, OLD_EMAIL, NEW_EMAIL));

        ArgumentCaptor<UserModel> recipient = ArgumentCaptor.forClass(UserModel.class);
        verify(emailTemplateProvider).setUser(recipient.capture());
        assertThat(recipient.getValue().getEmail()).isEqualTo(OLD_EMAIL);
        verify(emailTemplateProvider).send(anyString(), anyString(), anyMap());
    }

    @Test
    void ignoresUpdateProfileWithoutEmailChange() throws Exception {
        // No previous_email detail => the profile update did not change the email.
        provider.onEvent(emailChangeEvent(EventType.UPDATE_PROFILE, null, null));

        verify(session, never()).getProvider(EmailTemplateProvider.class);
        verify(emailTemplateProvider, never()).send(anyString(), anyString(), anyMap());
    }

    @Test
    void ignoresWhenPreviousEqualsUpdated() throws Exception {
        provider.onEvent(emailChangeEvent(EventType.UPDATE_EMAIL, NEW_EMAIL, NEW_EMAIL));

        verify(emailTemplateProvider, never()).send(anyString(), anyString(), anyMap());
    }

    @Test
    void ignoresUnrelatedUserEvents() {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(EventType.LOGIN);

        provider.onEvent(event);

        verify(session, never()).realms();
        verify(session, never()).users();
        verify(session, never()).getProvider(EmailTemplateProvider.class);
    }

    @Test
    void fallsBackToCurrentEmailWhenUpdatedDetailMissing() throws Exception {
        // UPDATE_EMAIL with previous but no updated detail still notifies; newEmail falls back to
        // the user's current (already-updated) address.
        provider.onEvent(emailChangeEvent(EventType.UPDATE_EMAIL, OLD_EMAIL, null));

        ArgumentCaptor<Map<String, Object>> attrs = ArgumentCaptor.forClass(Map.class);
        verify(emailTemplateProvider).send(anyString(), anyString(), attrs.capture());
        assertThat(attrs.getValue()).containsEntry("newEmail", NEW_EMAIL);
    }
}
