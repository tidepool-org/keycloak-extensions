package org.tidepool.keycloak.extensions.resource;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.SessionCodeChecks;
import org.keycloak.services.util.AuthenticationFlowURLHelper;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.tidepool.keycloak.extensions.model.RoleBean;

public class RegistrationsRealmResourceProvider implements RealmResourceProvider {

    private static final String PATH_REGISTRATIONS = "{realm}/" + RegistrationsRealmResourceProviderFactory.ID;
    private static final String PATH_RESTART = "restart";

    private KeycloakSession session;

    public RegistrationsRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Path(PATH_REGISTRATIONS)
    public RegistrationsRealmResourceProvider registrations(
            final @PathParam("realm") String name) {
        return this;
    }

    /**
     * Restart authentication with registration. Allows specification of role
     * (clinician or personal) to initiate registration.
     * 
     * Mimics LoginActionsService.restartSession, but restarts with registration
     * flow.
     * 
     * @param authSessionId
     * @param clientId
     * @param tabId
     * @param role
     * @return
     */
    @GET
    @Path(PATH_RESTART)
    public Response restart(
            final @QueryParam(LoginActionsService.AUTH_SESSION_ID) String authSessionId,
            final @QueryParam(Constants.CLIENT_ID) String clientId,
            final @QueryParam(Constants.TAB_ID) String tabId,
            final @QueryParam(RoleBean.PARAMETER_ROLE) String role) {

        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();
        HttpRequest request = context.getHttpRequest();
        ClientConnection clientConnection = context.getConnection();
        EventBuilder event = new EventBuilder(realm, session, clientConnection);

        event.event(EventType.RESTART_AUTHENTICATION);

        SessionCodeChecks checks = new SessionCodeChecks(realm, context.getUri(), request,
                clientConnection, session, event, authSessionId, null, null, clientId, tabId, null);

        AuthenticationSessionModel authenticationSession = checks.initialVerifyAuthSession();
        if (authenticationSession == null) {
            return checks.getResponse();
        }

        String flowPath = authenticationSession.getClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW);
        if (flowPath == null) {
            flowPath = LoginActionsService.REGISTRATION_PATH;
        }

        UserSessionModel userSession = new AuthenticationSessionManager(session).getUserSession(authenticationSession);
        if (userSession != null) {
            AuthenticationManager.backchannelLogout(session, userSession, false);
        }

        AuthenticationProcessor.resetFlow(authenticationSession, LoginActionsService.REGISTRATION_PATH);

        URI redirectUri = getLastExecutionUrl(flowPath, null, authenticationSession.getClient().getClientId(), tabId);

        if (role != null) {
            redirectUri = UriBuilder.fromUri(redirectUri).queryParam(RoleBean.PARAMETER_ROLE, role).build();
        }

        return Response.status(Response.Status.FOUND).location(redirectUri).build();
    }

    private URI getLastExecutionUrl(String flowPath, String executionId, String clientId, String tabId) {
        return new AuthenticationFlowURLHelper(session, session.getContext().getRealm(), session.getContext().getUri())
                .getLastExecutionUrl(flowPath, executionId, clientId, tabId);
    }
}
