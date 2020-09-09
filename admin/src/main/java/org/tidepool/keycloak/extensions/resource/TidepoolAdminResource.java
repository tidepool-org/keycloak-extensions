package org.tidepool.keycloak.extensions.resource;

import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TidepoolAdminResource extends AdminResource {

    private static final String ID_SEPARATOR = ",";

    private final KeycloakSession session;

    public TidepoolAdminResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("users")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getUsersById(@QueryParam("ids") String ids) {
        auth.users().requireQuery();
        auth.users().canView();

        List<UserRepresentation> representations = new ArrayList<>();
        if (ids != null) {
            RealmModel realm = session.getContext().getRealm();
            UserProvider provider = session.userStorageManager();

            for (String id : ids.split(ID_SEPARATOR)) {
                UserModel user = provider.getUserById(id, realm);
                if (user != null) {
                    representations.add(toRepresentation(user, realm));
                }
            }
        }

        return Response.status(Response.Status.OK).entity(representations).build();
    }

    private UserRepresentation toRepresentation(UserModel user, RealmModel realm) {
        UserRepresentation representation = ModelToRepresentation.toRepresentation(session, realm, user);
        representation.setRealmRoles(getRoles(user));
        representation.setCredentials(getCredentials(user, realm));
        return representation;
    }

    private List<String> getRoles(UserModel user) {
        return user.getRoleMappings().stream().map(RoleModel::getName).collect(Collectors.toList());
    }

    private List<CredentialRepresentation> getCredentials(UserModel user, RealmModel realm) {
        auth.users().requireManage(user);

        List<CredentialModel> models = session.userCredentialManager().getStoredCredentials(realm, user);
        models.forEach(c -> c.setSecretData(null));
        return models.stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
    }
}
