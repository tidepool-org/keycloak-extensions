package org.tidepool.keycloak.extensions.activity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderStorageProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.credential.OTPCredentialModel;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserActivityEventListenerProviderTest {

    private static final String REALM_ID = "realm-1";
    private static final String USER_ID = "user-1";
    private static final long EVENT_TIME = 1_700_000_000_000L;

    private KeycloakSession session;
    private EntityManager em;
    private SubjectCredentialManager credentials;
    private UserProvider users;
    private RealmModel realm;
    private UserModel user;
    private IdentityProviderStorageProvider idps;
    private UserActivityEventListenerProvider provider;

    @BeforeEach
    void setUp() {
        session = mock(KeycloakSession.class);
        em = mock(EntityManager.class);
        credentials = mock(SubjectCredentialManager.class);
        users = mock(UserProvider.class);
        realm = mock(RealmModel.class);
        user = mock(UserModel.class);
        idps = mock(IdentityProviderStorageProvider.class);

        RealmProvider realms = mock(RealmProvider.class);
        JpaConnectionProvider jpa = mock(JpaConnectionProvider.class);

        lenient().when(session.getProvider(JpaConnectionProvider.class)).thenReturn(jpa);
        lenient().when(jpa.getEntityManager()).thenReturn(em);
        lenient().when(session.realms()).thenReturn(realms);
        lenient().when(session.users()).thenReturn(users);
        lenient().when(session.identityProviders()).thenReturn(idps);
        lenient().when(realms.getRealm(REALM_ID)).thenReturn(realm);
        lenient().when(users.getUserById(realm, USER_ID)).thenReturn(user);
        lenient().when(user.credentialManager()).thenReturn(credentials);

        // No MFA credentials unless a test stubs them; fresh stream per invocation.
        lenient().when(credentials.getStoredCredentialsByTypeStream(anyString()))
                .thenAnswer(inv -> Stream.empty());

        provider = new UserActivityEventListenerProvider(session);
    }

    private Event event(EventType type) {
        return event(type, null);
    }

    private Event event(EventType type, String credentialType) {
        Event event = mock(Event.class);
        when(event.getType()).thenReturn(type);
        lenient().when(event.getRealmId()).thenReturn(REALM_ID);
        lenient().when(event.getUserId()).thenReturn(USER_ID);
        lenient().when(event.getTime()).thenReturn(EVENT_TIME);
        lenient().when(event.getDetails())
                .thenReturn(credentialType == null ? null : Map.of("credential_type", credentialType));
        return event;
    }

    private void stubHasOtp() {
        when(credentials.getStoredCredentialsByTypeStream(OTPCredentialModel.TYPE))
                .thenAnswer(inv -> Stream.of(mock(org.keycloak.credential.CredentialModel.class)));
    }

    private UserActivityEventEntity capturePersisted() {
        ArgumentCaptor<UserActivityEventEntity> captor = ArgumentCaptor.forClass(UserActivityEventEntity.class);
        verify(em).persist(captor.capture());
        return captor.getValue();
    }

    @Test
    void recordsLogin() {
        provider.onEvent(event(EventType.LOGIN));

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getEventType()).isEqualTo(UserActivityEventEntity.TYPE_LOGIN);
        assertThat(row.getRealmId()).isEqualTo(REALM_ID);
        assertThat(row.getUserId()).isEqualTo(USER_ID);
        assertThat(row.getEventTime()).isEqualTo(EVENT_TIME);
        assertThat(row.getId()).isNotBlank();
    }

    @Test
    void recordsMfaEnabledWhenSecondFactorAdded() {
        stubHasOtp(); // user now has a second factor

        provider.onEvent(event(EventType.UPDATE_CREDENTIAL, OTPCredentialModel.TYPE));

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getEventType()).isEqualTo(UserActivityEventEntity.TYPE_MFA_ENABLED);
    }

    @Test
    void recordsMfaDisabledWhenLastSecondFactorRemoved() {
        // No OTP remains after the removal -> disabled.
        provider.onEvent(event(EventType.REMOVE_CREDENTIAL, OTPCredentialModel.TYPE));

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getEventType()).isEqualTo(UserActivityEventEntity.TYPE_MFA_DISABLED);
    }

    @Test
    void skipsMfaDisabledWhenAnotherSecondFactorRemains() {
        stubHasOtp(); // removed one device but another second factor still exists

        provider.onEvent(event(EventType.REMOVE_CREDENTIAL, OTPCredentialModel.TYPE));

        verify(em, never()).persist(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skipsMfaEnabledWhenNoSecondFactorPresent() {
        // An add/update event but the user has no second factor (e.g. the added credential was not MFA);
        // must not emit MFA_ENABLED.
        provider.onEvent(event(EventType.UPDATE_CREDENTIAL, null));

        verify(em, never()).persist(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shortCircuitsNonMfaCredentialTypeWithoutTouchingUser() {
        // A password change carries credential_type=password; MFA state cannot have changed, so we
        // skip the user lookup and credential scan entirely.
        provider.onEvent(event(EventType.UPDATE_CREDENTIAL, "password"));

        verify(session, never()).users();
        verify(em, never()).persist(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotDisableForNonMfaCredentialRemoval() {
        // Removing a password (credential_type=password) is short-circuited: no MFA row even though the
        // user has no second factor.
        provider.onEvent(event(EventType.REMOVE_CREDENTIAL, "password"));

        verify(session, never()).users();
        verify(em, never()).persist(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordsIdpLinksSortedWithNamesAsJson() {
        FederatedIdentityModel google = federatedIdentity("google");
        FederatedIdentityModel apple = federatedIdentity("apple");
        when(users.getFederatedIdentitiesStream(realm, user)).thenReturn(Stream.of(google, apple));
        stubIdp("google", "Google");
        stubIdp("apple", "Apple");

        provider.onEvent(event(EventType.FEDERATED_IDENTITY_LINK));

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getEventType()).isEqualTo(UserActivityEventEntity.TYPE_IDP_LINKS_CHANGED);
        assertThat(row.getIdentityProviders())
                .isEqualTo("[{\"alias\":\"apple\",\"name\":\"Apple\"},{\"alias\":\"google\",\"name\":\"Google\"}]");
    }

    @Test
    void humanizesAliasWhenIdpHasNoDisplayName() {
        FederatedIdentityModel idp = federatedIdentity("google_tidepool-home");
        when(users.getFederatedIdentitiesStream(realm, user)).thenReturn(Stream.of(idp));
        stubIdp("google_tidepool-home", null); // no display name -> derive one from the alias

        provider.onEvent(event(EventType.FEDERATED_IDENTITY_LINK));

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getIdentityProviders())
                .isEqualTo("[{\"alias\":\"google_tidepool-home\",\"name\":\"Google Tidepool Home\"}]");
    }

    @Test
    void humanizesAliasWhenIdpModelMissing() {
        FederatedIdentityModel idp = federatedIdentity("azure-ad");
        when(users.getFederatedIdentitiesStream(realm, user)).thenReturn(Stream.of(idp));
        when(idps.getByAlias("azure-ad")).thenReturn(null); // IdP config gone but link remains

        provider.onEvent(event(EventType.FEDERATED_IDENTITY_LINK));

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getIdentityProviders()).isEqualTo("[{\"alias\":\"azure-ad\",\"name\":\"Azure Ad\"}]");
    }

    @Test
    void recordsEmptyIdpLinksWhenLastRemoved() {
        when(users.getFederatedIdentitiesStream(realm, user)).thenReturn(Stream.empty());

        provider.onEvent(event(EventType.REMOVE_FEDERATED_IDENTITY));

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getEventType()).isEqualTo(UserActivityEventEntity.TYPE_IDP_LINKS_CHANGED);
        assertThat(row.getIdentityProviders()).isEqualTo("[]");
    }

    @Test
    void ignoresUnrelatedEvents() {
        Event refresh = mock(Event.class);
        when(refresh.getType()).thenReturn(EventType.REFRESH_TOKEN);

        provider.onEvent(refresh);

        verify(em, never()).persist(org.mockito.ArgumentMatchers.any());
        verify(session, never()).users();
    }

    @Test
    void recordsMfaDisabledFromAdminCredentialDeletion() {
        // Admin deletes the user's last second factor; none remain -> disabled.
        provider.onEvent(adminEvent("users/" + USER_ID + "/credentials/cred-1", OperationType.DELETE), false);

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getEventType()).isEqualTo(UserActivityEventEntity.TYPE_MFA_DISABLED);
    }

    @Test
    void recordsMfaDisabledFromAdminDisableCredentialTypes() {
        provider.onEvent(adminEvent("users/" + USER_ID + "/disable-credential-types", OperationType.ACTION), false);

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getEventType()).isEqualTo(UserActivityEventEntity.TYPE_MFA_DISABLED);
    }

    @Test
    void skipsAdminCredentialDeletionWhenAnotherFactorRemains() {
        stubHasOtp(); // another second factor still exists after the deletion

        provider.onEvent(adminEvent("users/" + USER_ID + "/credentials/cred-1", OperationType.DELETE), false);

        verify(em, never()).persist(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresAdminCredentialRelabel() {
        // A non-DELETE credential operation (e.g. setting a label) is not a removal.
        provider.onEvent(adminEvent("users/" + USER_ID + "/credentials/cred-1/userLabel", OperationType.UPDATE), false);

        verify(em, never()).persist(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordsIdpLinksFromAdminLink() {
        FederatedIdentityModel google = federatedIdentity("google");
        when(users.getFederatedIdentitiesStream(realm, user)).thenReturn(Stream.of(google));
        stubIdp("google", "Google");

        provider.onEvent(adminEvent("users/" + USER_ID + "/federated-identity/google"), false);

        UserActivityEventEntity row = capturePersisted();
        assertThat(row.getEventType()).isEqualTo(UserActivityEventEntity.TYPE_IDP_LINKS_CHANGED);
        assertThat(row.getIdentityProviders()).isEqualTo("[{\"alias\":\"google\",\"name\":\"Google\"}]");
    }

    @Test
    void ignoresFailedAdminEvents() {
        AdminEvent failed = mock(AdminEvent.class);
        lenient().when(failed.getError()).thenReturn("could_not_delete");
        lenient().when(failed.getResourcePath()).thenReturn("users/" + USER_ID + "/credentials/cred-1");

        provider.onEvent(failed, false);

        verify(em, never()).persist(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresUnrelatedAdminEvents() {
        provider.onEvent(adminEvent("users/" + USER_ID + "/reset-password"), false);
        provider.onEvent(adminEvent("groups/group-1"), false);

        verify(em, never()).persist(org.mockito.ArgumentMatchers.any());
    }

    private AdminEvent adminEvent(String resourcePath) {
        return adminEvent(resourcePath, null);
    }

    private AdminEvent adminEvent(String resourcePath, OperationType operationType) {
        AdminEvent event = mock(AdminEvent.class);
        lenient().when(event.getError()).thenReturn(null);
        lenient().when(event.getRealmId()).thenReturn(REALM_ID);
        lenient().when(event.getResourcePath()).thenReturn(resourcePath);
        lenient().when(event.getOperationType()).thenReturn(operationType);
        lenient().when(event.getTime()).thenReturn(EVENT_TIME);
        return event;
    }

    private static FederatedIdentityModel federatedIdentity(String alias) {
        FederatedIdentityModel model = mock(FederatedIdentityModel.class);
        when(model.getIdentityProvider()).thenReturn(alias);
        return model;
    }

    private void stubIdp(String alias, String displayName) {
        IdentityProviderModel model = mock(IdentityProviderModel.class);
        lenient().when(model.getDisplayName()).thenReturn(displayName);
        when(idps.getByAlias(alias)).thenReturn(model);
    }
}
