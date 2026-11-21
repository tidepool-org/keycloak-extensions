package org.tidepool.keycloak.extensions.resource;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AdminApiAudienceMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The custom admin endpoints authenticate bearer tokens themselves, so they have to apply the Admin
 * API audience/scope check that Keycloak 26.8.2 added to its own Admin REST API (CVE-2026-19688).
 */
class AdminResourceTest {

    private static final String ADMIN_API_AUDIENCE = "https://auth.example.org/admin/realms/master";
    private static final String CLIENT_ID = "terraform";

    private KeycloakSession session;
    private KeycloakContext context;
    private RealmModel masterRealm;
    private RealmModel realmInContext;
    private AccessToken validatedToken;

    private MockedStatic<AppAuthManager> appAuthManager;
    private MockedConstruction<RealmManager> realmManagers;
    private MockedConstruction<AppAuthManager.BearerTokenAuthenticator> authenticators;
    private MockedStatic<AdminApiAudienceMapper> audienceMapper;
    private MockedStatic<AdminPermissions> adminPermissions;

    @BeforeEach
    void setUp() {
        stubJaxRsRuntime();
        session = mock(KeycloakSession.class);
        context = mock(KeycloakContext.class);
        masterRealm = mock(RealmModel.class);
        realmInContext = mock(RealmModel.class);
        ClientModel client = mock(ClientModel.class);

        HttpHeaders headers = mock(HttpHeaders.class);
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realmInContext);
        when(context.getRequestHeaders()).thenReturn(headers);
        when(masterRealm.getClientByClientId(CLIENT_ID)).thenReturn(client);

        validatedToken = new AccessToken();
        AuthenticationManager.AuthResult authResult = new AuthenticationManager.AuthResult(
                mock(UserModel.class), mock(UserSessionModel.class), validatedToken, client);

        // The real header parser consults the feature Profile (DPoP), which is not initialised here.
        appAuthManager = mockStatic(AppAuthManager.class);
        appAuthManager.when(() -> AppAuthManager.extractAuthorizationHeaderToken(headers))
                .thenReturn(unsignedToken());
        realmManagers = mockConstruction(RealmManager.class, (manager, ctx) -> {
            when(manager.getRealmByName("master")).thenReturn(masterRealm);
            when(manager.getKeycloakAdministrationRealm()).thenReturn(masterRealm);
        });
        authenticators = mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                (authenticator, ctx) -> when(authenticator.authenticate()).thenReturn(authResult));
        audienceMapper = mockStatic(AdminApiAudienceMapper.class);
        audienceMapper.when(() -> AdminApiAudienceMapper.getAdminApiAudience(session, masterRealm))
                .thenReturn(ADMIN_API_AUDIENCE);
        adminPermissions = mockStatic(AdminPermissions.class);
        adminPermissions.when(() -> AdminPermissions.evaluator(any(), any(), any()))
                .thenReturn(mock(AdminPermissionEvaluator.class));
    }

    @AfterEach
    void tearDown() {
        adminPermissions.close();
        audienceMapper.close();
        authenticators.close();
        realmManagers.close();
        appAuthManager.close();
        RuntimeDelegate.setInstance(null);
    }

    @Test
    void acceptsTokenIssuedForTheAdminApi() {
        validatedToken.setScope("openid kc-admin-api profile");
        TestAdminResource resource = new TestAdminResource(session, true);

        resource.setup();

        assertThat(resource.auth).isNotNull();
        verify(authenticator()).setAudience(ADMIN_API_AUDIENCE);
        assertRealmRestored();
    }

    @Test
    void rejectsTokenWithoutTheAdminApiScope() {
        validatedToken.setScope("openid profile");
        TestAdminResource resource = new TestAdminResource(session, true);

        assertThatThrownBy(resource::setup).isInstanceOf(NotAuthorizedException.class);

        assertThat(resource.auth).isNull();
        verify(authenticator()).setAudience(ADMIN_API_AUDIENCE);
        assertRealmRestored();
    }

    @Test
    void rejectsTokenFailingAudienceValidation() {
        // BearerTokenAuthenticator returns null when the audience does not match.
        validatedToken.setScope("kc-admin-api");
        authenticators.close();
        authenticators = mockConstruction(AppAuthManager.BearerTokenAuthenticator.class,
                (authenticator, ctx) -> when(authenticator.authenticate()).thenReturn(null));
        TestAdminResource resource = new TestAdminResource(session, true);

        assertThatThrownBy(resource::setup).isInstanceOf(NotAuthorizedException.class);

        assertThat(resource.auth).isNull();
        assertRealmRestored();
    }

    @Test
    void skipsAudienceAndScopeChecksWhenDisabled() {
        validatedToken.setScope("openid profile");
        TestAdminResource resource = new TestAdminResource(session, false);

        resource.setup();

        assertThat(resource.auth).isNotNull();
        verify(authenticator(), never()).setAudience(any());
        assertRealmRestored();
    }

    private AppAuthManager.BearerTokenAuthenticator authenticator() {
        assertThat(authenticators.constructed()).hasSize(1);
        return authenticators.constructed().get(0);
    }

    private void assertRealmRestored() {
        InOrder order = inOrder(context);
        order.verify(context).setRealm(masterRealm);
        order.verify(context).setRealm(realmInContext);
    }

    /** Building a NotAuthorizedException needs a JAX-RS runtime, which only exists inside Keycloak. */
    private static void stubJaxRsRuntime() {
        Response unauthorized = mock(Response.class);
        when(unauthorized.getStatus()).thenReturn(401);
        when(unauthorized.getStatusInfo()).thenReturn(Response.Status.UNAUTHORIZED);
        Response.ResponseBuilder builder = mock(Response.ResponseBuilder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(unauthorized);
        RuntimeDelegate delegate = mock(RuntimeDelegate.class);
        when(delegate.createResponseBuilder()).thenReturn(builder);
        RuntimeDelegate.setInstance(delegate);
    }

    /** setup() only reads the issuer and azp before the (mocked) authenticator validates the token. */
    private static String unsignedToken() {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(
                ("{\"iss\":\"https://auth.example.org/realms/master\",\"azp\":\"" + CLIENT_ID + "\"}")
                        .getBytes(StandardCharsets.UTF_8));
        String signature = encoder.encodeToString("signature".getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + "." + signature;
    }

    private static final class TestAdminResource extends AdminResource {
        private final boolean audienceCheckEnabled;

        TestAdminResource(KeycloakSession session, boolean audienceCheckEnabled) {
            super(session);
            this.audienceCheckEnabled = audienceCheckEnabled;
        }

        @Override
        protected boolean isAudienceCheckEnabled() {
            return audienceCheckEnabled;
        }
    }
}
