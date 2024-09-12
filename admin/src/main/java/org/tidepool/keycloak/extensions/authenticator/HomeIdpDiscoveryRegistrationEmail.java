package org.tidepool.keycloak.extensions.authenticator;

import de.sventorben.keycloak.authentication.hidpd.AbstractHomeIdpDiscoveryAuthenticatorFactory;
import de.sventorben.keycloak.authentication.hidpd.discovery.spi.HomeIdpDiscoverer;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationSelectionOption;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.FlowStatus;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.authentication.forms.RegistrationPage;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class HomeIdpDiscoveryRegistrationEmail implements FormAction {

    public static final String ERROR_EMAIL_BOUND_TO_IDP = "emailBoundToIdp";

    private final AbstractHomeIdpDiscoveryAuthenticatorFactory.DiscovererConfig discovererConfig;

    HomeIdpDiscoveryRegistrationEmail(AbstractHomeIdpDiscoveryAuthenticatorFactory.DiscovererConfig discovererConfig) {
        this.discovererConfig = discovererConfig;
    }

    @Override
    public void validate(ValidationContext validationContext) {
        MultivaluedMap<String, String> formData = validationContext.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();
        validationContext.getEvent().detail(Details.REGISTER_METHOD, "form");

        if (formData.getFirst(RegistrationPage.FIELD_EMAIL) != null) {
            AuthenticationFlowContext adapter = new AuthenticationFlowContextAdapter(validationContext);
            HomeIdpDiscoverer discoverer = validationContext.getSession().getProvider(HomeIdpDiscoverer.class, discovererConfig.getProviderId());
            List<IdentityProviderModel> idp = discoverer.discoverForUser(adapter, formData.getFirst(RegistrationPage.FIELD_EMAIL));
            if (!idp.isEmpty()) {
                errors.add(new FormMessage(RegistrationPage.FIELD_EMAIL, ERROR_EMAIL_BOUND_TO_IDP));
            }
        }

        if (!errors.isEmpty()) {
            validationContext.error(Errors.INVALID_REGISTRATION);
            validationContext.validationError(formData, errors);
        } else {
            validationContext.success();
        }
    }

    @Override
    public void success(FormContext context) {

    }

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {

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

    public static class AuthenticationFlowContextAdapter implements AuthenticationFlowContext {

        private final ValidationContext context;

        public AuthenticationFlowContextAdapter(ValidationContext context) {
            this.context = context;
        }

        @Override
        public UserModel getUser() {
            return null;
        }

        @Override
        public void setUser(UserModel userModel) {

        }

        @Override
        public List<AuthenticationSelectionOption> getAuthenticationSelections() {
            return null;
        }

        @Override
        public void setAuthenticationSelections(List<AuthenticationSelectionOption> list) {

        }

        @Override
        public void clearUser() {

        }

        @Override
        public void attachUserSession(UserSessionModel userSessionModel) {

        }

        @Override
        public AuthenticationSessionModel getAuthenticationSession() {
            return null;
        }

        @Override
        public String getFlowPath() {
            return null;
        }

        @Override
        public LoginFormsProvider form() {
            return null;
        }

        @Override
        public URI getActionUrl(String s) {
            return null;
        }

        @Override
        public URI getActionTokenUrl(String s) {
            return null;
        }

        @Override
        public URI getRefreshExecutionUrl() {
            return null;
        }

        @Override
        public URI getRefreshUrl(boolean b) {
            return null;
        }

        @Override
        public void cancelLogin() {

        }

        @Override
        public void resetFlow() {

        }

        @Override
        public void resetFlow(Runnable runnable) {

        }

        @Override
        public void fork() {

        }

        @Override
        public void forkWithSuccessMessage(FormMessage formMessage) {

        }

        @Override
        public void forkWithErrorMessage(FormMessage formMessage) {

        }

        @Override
        public EventBuilder getEvent() {
            return null;
        }

        @Override
        public EventBuilder newEvent() {
            return null;
        }

        @Override
        public AuthenticationExecutionModel getExecution() {
            return null;
        }

        @Override
        public AuthenticationFlowModel getTopLevelFlow() {
            return AuthenticatorUtil.getTopParentFlow(context.getRealm(), context.getExecution());
        }

        @Override
        public RealmModel getRealm() {
            return context.getRealm();
        }

        @Override
        public ClientConnection getConnection() {
            return null;
        }

        @Override
        public UriInfo getUriInfo() {
            return null;
        }

        @Override
        public KeycloakSession getSession() {
            return context.getSession();
        }

        @Override
        public HttpRequest getHttpRequest() {
            return null;
        }

        @Override
        public BruteForceProtector getProtector() {
            return null;
        }

        @Override
        public AuthenticatorConfigModel getAuthenticatorConfig() {
            return null;
        }

        @Override
        public FormMessage getForwardedErrorMessage() {
            return null;
        }

        @Override
        public FormMessage getForwardedSuccessMessage() {
            return null;
        }

        @Override
        public FormMessage getForwardedInfoMessage() {
            return null;
        }

        @Override
        public void setForwardedInfoMessage(String s, Object... objects) {

        }

        @Override
        public String generateAccessCode() {
            return null;
        }

        @Override
        public AuthenticationExecutionModel.Requirement getCategoryRequirementFromCurrentFlow(String s) {
            return null;
        }

        @Override
        public void success() {

        }

        @Override
        public void failure(AuthenticationFlowError authenticationFlowError) {

        }

        @Override
        public void failure(AuthenticationFlowError authenticationFlowError, Response response) {

        }

        @Override
        public void failure(AuthenticationFlowError authenticationFlowError, Response response, String s, String s1) {

        }

        @Override
        public void challenge(Response response) {

        }

        @Override
        public void forceChallenge(Response response) {

        }

        @Override
        public void failureChallenge(AuthenticationFlowError authenticationFlowError, Response response) {

        }

        @Override
        public void attempted() {

        }

        @Override
        public FlowStatus getStatus() {
            return null;
        }

        @Override
        public AuthenticationFlowError getError() {
            return null;
        }

        @Override
        public String getEventDetails() {
            return null;
        }

        @Override
        public String getUserErrorMessage() {
            return null;
        }
    }
}
