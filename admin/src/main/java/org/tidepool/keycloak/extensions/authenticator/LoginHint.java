package org.tidepool.keycloak.extensions.authenticator;

import de.sventorben.keycloak.authentication.hidpd.Users;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;
import java.util.stream.Collectors;

import static org.keycloak.protocol.oidc.OIDCLoginProtocol.LOGIN_HINT_PARAM;

// This file is a copy of org.tidepool.keycloak.extensions.authenticator.LoginHint with class and method access modifiers
// changed to public. This is required to allow HomeIdpDiscoveryLoginHintAuthenticator use this class
// and avoid maintaining our own fork.
public final class LoginHint {

    private final AuthenticationFlowContext context;
    private final Users users;

    public LoginHint(AuthenticationFlowContext context, Users users) {
        this.context = context;
        this.users = users;
    }

    public void setInAuthSession(IdentityProviderModel homeIdp, String username) {
        String loginHint = username;
        UserModel user = users.lookupBy(username);
        if (user != null) {
            Map<String, String> idpToUsername = context.getSession().users()
                .getFederatedIdentitiesStream(context.getRealm(), user)
                .collect(
                    Collectors.toMap(FederatedIdentityModel::getIdentityProvider,
                        FederatedIdentityModel::getUserName));
            String alias = homeIdp == null ? "" : homeIdp.getAlias();
            loginHint = idpToUsername.getOrDefault(alias, username);
        }
        setInAuthSession(loginHint);
    }

    void setInAuthSession(String loginHint) {
        context.getAuthenticationSession().setClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
    }

    public String getFromSession() {
        return context.getAuthenticationSession().getClientNote(LOGIN_HINT_PARAM);
    }

    void copyTo(ClientSessionCode<AuthenticationSessionModel> clientSessionCode) {
        String loginHint = getFromSession();
        if (clientSessionCode.getClientSession() != null && loginHint != null) {
            clientSessionCode.getClientSession().setClientNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        }
    }
}
