package org.tidepool.keycloak.extensions.model;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.Urls;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.tidepool.keycloak.extensions.resource.RegistrationsRealmResourceProvider;

public class RoleBean {

    public static final String ROLE_CLINICIAN = "clinician";
    public static final String ROLE_CLINIC_DEPRECATED = "clinic";
    public static final String ROLE_PATIENT = "patient";

    public static final List<String> ROLES_LIST = Arrays.asList(ROLE_CLINICIAN, ROLE_PATIENT);
    public static final Set<String> ROLES_SET = new HashSet<>(ROLES_LIST);

    public static final List<String> ROLES_CLINICIAN_LIST = Arrays.asList(ROLE_CLINICIAN, ROLE_CLINIC_DEPRECATED);
    public static final Set<String> ROLES_CLINICIAN_SET = new HashSet<>(ROLES_CLINICIAN_LIST);

    public static final String PARAMETER_ROLE = "role";

    public static final String AUTH_NOTE_ROLE = "role";

    private final RealmModel realm;
    private final URI baseUri;
    private final AuthenticationFlowContext context;
    private final AuthenticationSessionModel authenticationSession;

    public RoleBean(RealmModel realm, URI baseUri, AuthenticationFlowContext context,
            AuthenticationSessionModel authenticationSession) {
        this.realm = realm;
        this.baseUri = baseUri;
        this.context = context;
        this.authenticationSession = authenticationSession;
    }

    public boolean hasClinicianRole() {
        if (hasClinicianRoleFromAuthenticationSession(authenticationSession)) {
            return true;
        }
        if (context != null && hasClinicianRoleFromRealmUser(context.getRealm(), context.getUser())) {
            return true;
        }
        return false;
    }

    public static boolean hasClinicianRoleFromAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        if (authenticationSession != null) {
            return ROLES_CLINICIAN_SET.contains(authenticationSession.getAuthNote(AUTH_NOTE_ROLE));
        }
        return false;
    }

    public static boolean hasClinicianRoleFromRealmUser(RealmModel realm, UserModel user) {
        if (realm != null && user != null) {
            for (String clinicianRole : ROLES_CLINICIAN_SET) {
                RoleModel clinicianRoleModel = realm.getRole(clinicianRole);
                if (user.hasRole(clinicianRoleModel)) {
                    return true;
                }
            }
        }
        return false;
    }

    public URI getRegistrationUriForClinicianRole() {
        return getRegistrationUriForRole(RoleBean.ROLE_CLINICIAN);
    }

    public URI getRegistrationUriForPatientRole() {
        return getRegistrationUriForRole(RoleBean.ROLE_PATIENT);
    }

    private URI getRegistrationUriForRole(String role) {
        return Urls
                .realmBase(baseUri)
                .path(RegistrationsRealmResourceProvider.class, "registrations")
                .path(RegistrationsRealmResourceProvider.class, "restart")
                .queryParam(PARAMETER_ROLE, role)
                .build(getRealmName());

    }

    private String getRealmName() {
        return realm != null ? realm.getName() : null;
    }
}
