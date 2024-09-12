package org.tidepool.keycloak.extensions.authenticator;

import de.sventorben.keycloak.authentication.hidpd.AbstractHomeIdpDiscoveryAuthenticatorFactory;
import de.sventorben.keycloak.authentication.hidpd.Users;
import de.sventorben.keycloak.authentication.hidpd.discovery.spi.HomeIdpDiscoverer;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.List;

import static org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME;
import static org.keycloak.protocol.oidc.OIDCLoginProtocol.LOGIN_HINT_PARAM;

final class HomeIdpDiscoveryLoginHintAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator  {

    private static final Logger LOG = Logger.getLogger(HomeIdpDiscoveryLoginHintAuthenticator.class);

    private final AbstractHomeIdpDiscoveryAuthenticatorFactory.DiscovererConfig discovererConfig;

    HomeIdpDiscoveryLoginHintAuthenticator(AbstractHomeIdpDiscoveryAuthenticatorFactory.DiscovererConfig discovererConfig) {
        this.discovererConfig = discovererConfig;
    }

    @Override
    public void authenticate(AuthenticationFlowContext authenticationFlowContext) {
        LoginHint hidpdLoginHint = new LoginHint(authenticationFlowContext, new Users(authenticationFlowContext.getSession()));
        Redirector hidpdRedirector = new Redirector(authenticationFlowContext);

        String loginHint = trimToNull(hidpdLoginHint.getFromSession());
        if (loginHint != null) {
            String username = setUserInContext(authenticationFlowContext, loginHint);
            // Only redirect to the IDP if the user doesn't exist
            if (authenticationFlowContext.getUser() == null) {
                HomeIdpDiscoverer discoverer = authenticationFlowContext.getSession().getProvider(HomeIdpDiscoverer.class, discovererConfig.getProviderId());
                final List<IdentityProviderModel> homeIdps = discoverer.discoverForUser(authenticationFlowContext, username);
                if (homeIdps.size() == 1) {
                    IdentityProviderModel homeIdp = homeIdps.get(0);
                    hidpdLoginHint.setInAuthSession(homeIdp, username);
                    hidpdRedirector.redirectTo(homeIdp);
                    return;
                }
            }
        }

        authenticationFlowContext.clearUser();
        authenticationFlowContext.attempted();
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext) {
    }

    private String setUserInContext(AuthenticationFlowContext context, String username) {
        context.clearUser();

        username = trimToNull(username);

        if (username == null) {
            LOG.warn("No or empty username found in request");
            context.getEvent().error(Errors.USER_NOT_FOUND);
            context.attempted();
            return null;
        }

        LOG.debugf("Found username '%s' in request", username);
        context.getEvent().detail(Details.USERNAME, username);
        context.getAuthenticationSession().setAuthNote(ATTEMPTED_USERNAME, username);
        context.getAuthenticationSession().setClientNote(LOGIN_HINT_PARAM, username);

        try {
            UserModel user = KeycloakModelUtils.findUserByNameOrEmail(context.getSession(), context.getRealm(),
                username);
            if (user != null) {
                LOG.tracef("Setting user '%s' in context", user.getId());
                context.setUser(user);
            }
        } catch (ModelDuplicateException ex) {
            LOG.warnf(ex, "Could not uniquely identify the user. Multiple users with name or email '%s' found.",
                username);
        }

        return username;
    }

    private String trimToNull(String username) {
        if (username != null) {
            username = username.trim();
            if ("".equalsIgnoreCase(username))
                username = null;
        }
        return username;
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
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {

    }
}
