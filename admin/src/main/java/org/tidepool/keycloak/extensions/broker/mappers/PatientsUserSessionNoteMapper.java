package org.tidepool.keycloak.extensions.broker.mappers;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Patient;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.managers.AuthenticationManager;
import org.tidepool.keycloak.extensions.broker.FHIRContext;
import org.tidepool.keycloak.extensions.broker.SMARTIdentityProvider;
import org.tidepool.keycloak.extensions.broker.SMARTIdentityProviderFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.tidepool.keycloak.extensions.authenticator.SMARTIdentityProviderAuthenticator.CORRELATION_ID;


public class PatientsUserSessionNoteMapper extends AbstractIdentityProviderMapper {

    private static final Logger LOG = Logger.getLogger(PatientsUserSessionNoteMapper.class);

    private static final String PATIENTS_NOTE_NAME = "smart/patients";

    private static final String[] COMPATIBLE_PROVIDERS = { ANY_PROVIDER };

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES =
            new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    public static final String PROVIDER_ID = "smart-patients-session-note-idp-mapper";

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
        return "Patient User Session Note Mapper";
    }

    @Override
    public String getHelpText() {
        return "Adds the patient in context to the user session note.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
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
        addPatientToSessionNote(session, realm, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
                                   IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        addPatientToSessionNote(session, realm, context);
    }

    private void addPatientToSessionNote(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context) {
        String correlationId = context.getAuthenticationSession().getClientNote(CORRELATION_ID);
        if (correlationId.isBlank()) {
            LOG.warnf("Client correlationId is not defined for brokered user %s", context.getBrokerUserId());
            return;
        }

        AccessTokenResponse accessTokenResponse = (AccessTokenResponse) context.getContextData().get(OIDCIdentityProvider.FEDERATED_ACCESS_TOKEN_RESPONSE);
        Object patientId = accessTokenResponse.getOtherClaims().getOrDefault("patient", "");
        if (!(patientId instanceof String) || ((String)patientId).isBlank()) {
            LOG.warnf("Patient id not found in access token response for brokered user %s", context.getBrokerUserId());
            return;
        }

        String fhirVersion = (String) context.getContextData().get(SMARTIdentityProvider.FHIR_VERSION);
        String fhirBaseURL = (String) context.getContextData().get(SMARTIdentityProvider.FHIR_BASE_URL);

        IGenericClient client = FHIRContext.getFHIRClient(fhirVersion, fhirBaseURL, accessTokenResponse.getToken());
        Patient patient = client.read().resource(Patient.class).withId((String)patientId).execute();

        PatientsUserSessionNote patients;
        String noteValue;

        // There may be multiple authentication sessions for a single SSO session.
        // Retrieve the session notes from the user session, to make sure we are appending
        // to the list of patients associated to the SSO session, not to the current auth session.
        UserSessionModel userSession = this.getUserSession(session, realm);
        if (userSession != null) {
            noteValue = userSession.getNote(PATIENTS_NOTE_NAME);
            try {
                patients = PatientsUserSessionNote.deserializeFromString(noteValue);
            } catch (IOException e) {
                LOG.warnf("Unable to deserialize patient notes: %s", noteValue);
                return;
            }
        } else {
            patients = new PatientsUserSessionNote();
        }

        patients.addPatient(correlationId, patient);
        try {
            context.setSessionNote(PATIENTS_NOTE_NAME, patients.serializeAsString());
        } catch (IOException e) {
            LOG.warnf("Unable to serialize patient notes: %s", e.getMessage());
        }
    }

    private UserSessionModel getUserSession(KeycloakSession session, RealmModel realm) {
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, realm, true);
        if (authResult == null) {
            return null;
        }
        return authResult.getSession();
    }
}

