package org.tidepool.keycloak.extensions.authenticator;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.FlowStatus;
import org.keycloak.authentication.authenticators.broker.IdpDetectExistingBrokerUserAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.AuthenticatorConfigModel;

import java.util.Optional;

public class IdpDetectExistingBrokerUserAuthenticatorCustomError extends IdpDetectExistingBrokerUserAuthenticator {

    private static final Logger LOG = Logger.getLogger(IdpDetectExistingBrokerUserAuthenticatorCustomError.class);

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        super.authenticateImpl(context, serializedCtx, brokerContext);

        if (context.getStatus() != FlowStatus.CHALLENGE) {
            LOG.trace("skipping authenticator execution with non-challenge status");
            return;
        }

        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig == null || authConfig.getConfig() == null) {
            LOG.debugf("No authenticator config found");
            return;
        }

        String customErrorMessage = authConfig.getConfig().get(IdpDetectExistingBrokerUserAuthenticatorCustomErrorFactory.CONF_ERROR_MESSAGE);
        if (customErrorMessage == null || customErrorMessage.isBlank()) {
            LOG.debugf("No custom error message configured, using the default");
            return;
        }

        String username = getUsername(context, serializedCtx, brokerContext);
        Optional.ofNullable(context.getAuthenticatorConfig())
            .map(AuthenticatorConfigModel::getConfig)
            .map(f -> f.get(IdpDetectExistingBrokerUserAuthenticatorCustomErrorFactory.CONF_ERROR_MESSAGE))
            .filter(m -> !m.isBlank())
            .ifPresent(message -> {
                LOG.warnf("User %s doesn't exist in the realm, returning custom error message", username);
                context.challenge(
                    context.form()
                        .setError(message, username, brokerContext.getIdpConfig().getAlias())
                        .createErrorPage(Response.Status.UNAUTHORIZED)
                );
            });
    }
}
