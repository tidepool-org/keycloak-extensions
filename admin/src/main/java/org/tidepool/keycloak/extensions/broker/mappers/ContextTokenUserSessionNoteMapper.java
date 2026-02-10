package org.tidepool.keycloak.extensions.broker.mappers;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Patient;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.managers.AuthenticationManager;
import org.tidepool.keycloak.extensions.broker.FHIRContext;
import org.tidepool.keycloak.extensions.broker.SMARTIdentityProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.tidepool.keycloak.extensions.authenticator.SMARTIdentityProviderAuthenticator.CORRELATION_ID;


public class ContextTokenUserSessionNoteMapper extends AbstractIdentityProviderMapper {

    private static final Logger LOG = Logger.getLogger(ContextTokenUserSessionNoteMapper.class);

    private static final String CONTEXT_TOKENS_PREFIX = "smart.context_tokens.";

    private static final String[] COMPATIBLE_PROVIDERS = { ANY_PROVIDER };

    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES =
            new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    public static final String PROVIDER_ID = "smart-tokens-session-note-idp-mapper";

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "User Session";
    }

    @Override
    public String getDisplayType() {
        return "SMART Context Tokens Session Note Mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds the context tokens to the user session note.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty token = new ProviderConfigProperty();
        token.setType(ProviderConfigProperty.STRING_TYPE);
        token.setName("token");
        token.setLabel("Context Token");
        token.setHelpText("The key of the context token");
        token.setRequired(true);

        return List.of(token);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
                              IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String token = mapperModel.getConfig().get("token");
        processContextTokens(session, realm, context, token);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
                                   IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        String token = mapperModel.getConfig().get("token");
        processContextTokens(session, realm, context, token);
    }

    private void processContextTokens(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context, String token) {
        AccessTokenResponse accessTokenResponse = (AccessTokenResponse) context.getContextData().get(OIDCIdentityProvider.FEDERATED_ACCESS_TOKEN_RESPONSE);
        Object value = accessTokenResponse.getOtherClaims().getOrDefault(token, null);
        if (!(value instanceof String) || ((String)value).isBlank()) {
            LOG.warnf("The token %s was not found in the federated access token response", token);
            return;
        }

        context.setSessionNote(CONTEXT_TOKENS_PREFIX + token, (String) value);
    }
}

