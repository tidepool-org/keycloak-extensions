package org.tidepool.keycloak.extensions.authenticator;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.services.Urls;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

final class RedirectToRegistrationPage implements Authenticator {

    RedirectToRegistrationPage() {
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        URI baseURI = prepareBaseUriBuilder(context).build();
        URI register = Urls.realmRegisterPage(baseURI, context.getRealm().getName());
        context.forceChallenge(Response.seeOther(register).build());
    }

    private UriBuilder prepareBaseUriBuilder(AuthenticationFlowContext context) {
        UriBuilder uriBuilder = context.getUriInfo().getBaseUriBuilder();
        ClientModel client = context.getSession().getContext().getClient();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        uriBuilder.replaceQuery(null);

        if (client != null) {
            uriBuilder.queryParam(Constants.CLIENT_ID, client.getClientId());
        }
        if (authSession != null) {
            uriBuilder.queryParam(Constants.TAB_ID, authSession.getTabId());
        }
        return uriBuilder;
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel) {

    }

    @Override
    public void close() {

    }
}
