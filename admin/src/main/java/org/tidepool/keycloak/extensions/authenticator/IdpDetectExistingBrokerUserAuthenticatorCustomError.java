package org.tidepool.keycloak.extensions.authenticator;

import jakarta.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.IdpDetectExistingBrokerUserAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.AuthenticatorConfigModel;

import java.util.Optional;

public class IdpDetectExistingBrokerUserAuthenticatorCustomError extends IdpDetectExistingBrokerUserAuthenticator {
    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        super.authenticateImpl(context, serializedCtx, brokerContext);

        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        if (authConfig == null || authConfig.getConfig() == null) {
            return;
        }

        String customErrorMessage = authConfig.getConfig().get(IdpDetectExistingBrokerUserAuthenticatorCustomErrorFactory.CONF_ERROR_MESSAGE);
        if (customErrorMessage == null || customErrorMessage.isBlank()) {
            return;
        }

        String username = brokerContext.getUsername();
        Optional.ofNullable(context.getAuthenticatorConfig())
            .map(AuthenticatorConfigModel::getConfig)
            .map(f -> f.get(IdpDetectExistingBrokerUserAuthenticatorCustomErrorFactory.CONF_ERROR_MESSAGE))
            .filter(m -> !m.isBlank())
            .ifPresent(message ->
                context.challenge(
                    context.form()
                        .setError(message, username, brokerContext.getIdpConfig().getAlias())
                        .createErrorPage(Response.Status.UNAUTHORIZED)
                )
            );
    }
}
