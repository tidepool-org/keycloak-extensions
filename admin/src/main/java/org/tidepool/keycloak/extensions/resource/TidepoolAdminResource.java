package org.tidepool.keycloak.extensions.resource;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.keycloak.models.*;
import org.keycloak.validate.Validators;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.DefaultJpaConnectionProviderFactory;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TidepoolAdminResource extends AdminResource {

    private static final String ID_SEPARATOR = ",";
    private static final Pattern UNCLAIMED_CUSTODIAL = Pattern.compile("unclaimed-custodial-automation\\+\\d+@tidepool\\.org", Pattern.CASE_INSENSITIVE);

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
            for (String id : ids.split(ID_SEPARATOR)) {
                UserModel user = session.users().getUserById(realm, id);
                if (user != null) {
                    representations.add(toRepresentation(user, realm));
                }
            }
        }

        return Response.status(Response.Status.OK).entity(representations).build();
    }

    @POST
    @Path("unlink-federated-user/{userId}")
    public Response unlinkFederatedUser(@PathParam("userId") final String userId) {
        auth.users().canManage();

        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        if (user.getFederationLink() == null) {
            throw new BadRequestException("User is not a federated user");
        }
        user.setFederationLink(null);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    // This will clone the user, known as the child, with an id of userId to
    // have a new parent that has the child's previous username and email,
    // while the child will have the new email and username from the newUsername
    // field of the POST body
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.APPLICATION_JSON})
    @Path("clone-user/{userId}")
    public Response cloneUser(@PathParam("userId") final String userId, final CloneUserBody body) {
        auth.users().canManage();

        String newUsername = body.newUsername;
        if (!TidepoolAdminResource.UNCLAIMED_CUSTODIAL.matcher(newUsername).find()) {
            throw new BadRequestException("newUsername must conform to the unclaimed custodial email format");
        }
        if (body.custodianRoleName == null || body.custodianRoleName.isBlank()) {
            throw new BadRequestException("custodianRoleName must not be empty");
        }

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            throw new NotFoundException("User not found.");
        }
        boolean alreadyMigrated = user.getUsername() != null && TidepoolAdminResource.UNCLAIMED_CUSTODIAL.matcher(user.getUsername()).find();
        if (alreadyMigrated) {
            throw new BadRequestException(String.format("user %s already migrated", userId));
        }

        String custodianRoleName = body.custodianRoleName.trim();
        String newParentUserId = KeycloakModelUtils.generateId();
        String parentUsername = user.getUsername();
        String childUserId = userId;

        boolean roleFound = session.roles().getRealmRolesStream(realm).anyMatch(role -> custodianRoleName.equals(role.getName()));
        if (!roleFound) {
            throw new BadRequestException(String.format("Realm role \"%s\" not found.", custodianRoleName));
        }

        JpaConnectionProvider connProvider = session.getProvider(JpaConnectionProvider.class);
        if (connProvider == null) {
            throw new InternalServerErrorException("Unable to get persistence connection provider.");
        }
        // EntityManager is not thread safe so create a new application managed EntityManager
        EntityManager em = connProvider.getEntityManager().getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        // Update the child to have the new username and email of newUsername
        em.createNativeQuery("UPDATE user_entity SET email = ?1, email_constraint = ?1, username = ?1 WHERE id = ?2").
            setParameter(1, newUsername).
            setParameter(2, childUserId).
            executeUpdate();

        // Create a new parent user with the same properties as the child
        // except the parent will now assume the child's previous email /
        // username.
        em.createNativeQuery("INSERT INTO user_entity(id, email, email_constraint, email_verified, enabled, federation_link, first_name, last_name, realm_id, username, created_timestamp, service_account_client_link, not_before) SELECT ?1, ?2, ?2, email_verified, enabled, federation_link, first_name, last_name, realm_id, ?2, created_timestamp, service_account_client_link, not_before FROM user_entity WHERE id = ?3").
            setParameter(1, newParentUserId).
            setParameter(2, parentUsername).
            setParameter(3, childUserId).
            executeUpdate();

        // Copy over the credentials of the child to the parent so the parent can login with the same credentials.
        em.createNativeQuery("INSERT INTO credential(id, salt, type, user_id, created_date, user_label, secret_data, credential_data, priority) SELECT ?1, salt, type, ?2, created_date, user_label, secret_data, credential_data, priority FROM credential WHERE user_id = ?3").
            setParameter(1, KeycloakModelUtils.generateId()).
            setParameter(2, newParentUserId).
            setParameter(3, childUserId).
            executeUpdate();

        // copy over role mappings
        em.createNativeQuery("INSERT INTO user_role_mapping(role_id, user_id) SELECT role_id, ?1 FROM user_role_mapping WHERE user_id = ?2").
            setParameter(1, newParentUserId).
            setParameter(2, childUserId).
            executeUpdate();

        // Add custodian role (this can be any valid existing role in the
        // realm, carepartner, new we created, etc)
        em.createNativeQuery("INSERT INTO user_role_mapping(role_id, user_id) SELECT id, ?1 FROM keycloak_role WHERE name = ?2 AND realm_id = ?3").
            setParameter(1, newParentUserId).
            setParameter(2, custodianRoleName).
            setParameter(3, realm.getId()).
            executeUpdate();

        // copy over required actions
        em.createNativeQuery("INSERT INTO user_required_action(user_id, required_action) SELECT ?1, required_action FROM user_required_action WHERE user_id = ?2").
            setParameter(1, newParentUserId).
            setParameter(2, childUserId).
            executeUpdate();

        // Only set the fullName attribute from the child's custodian's fullName
        em.createNativeQuery("INSERT INTO user_attribute(name, value, user_id, id) SELECT ?1, value, ?2, ?3 FROM user_attribute WHERE user_id = ?4 AND name = ?5").
            setParameter(1, "full_name").
            setParameter(2, newParentUserId).
            setParameter(3, KeycloakModelUtils.generateId()).
            setParameter(4, childUserId).
            setParameter(5, "custodian_full_name").
            executeUpdate();

        // copy over group memberships
        em.createNativeQuery("INSERT INTO user_group_membership(group_id, user_id) SELECT group_id, ?1 FROM user_group_membership WHERE user_id = ?2").
            setParameter(1, newParentUserId).
            setParameter(2, childUserId).
            executeUpdate();

        tx.commit();

        // The Keycloak 24+ modules have removed cache eviction methods, so instead set child user's email and username "again" through the model. If we got this far,
        // the previous transaction has succeeded so this is "safe" and will cause a user updated event which will clear the cache entry for the given user.
        user.setEmail(newUsername);
        user.setUsername(newUsername);

        UserModel parentUser = session.users().getUserById(realm, newParentUserId);
        if (parentUser == null) {
            throw new InternalServerErrorException("unable to retrieve cloned user");
        }
        return Response.status(Response.Status.CREATED).entity(toRepresentation(parentUser, realm)).build();
    }

    private UserRepresentation toRepresentation(UserModel user, RealmModel realm) {
        UserRepresentation representation = ModelToRepresentation.toRepresentation(session, realm, user);
        representation.setRealmRoles(getRoles(user));
        representation.setCredentials(getCredentials(user));
        return representation;
    }

    private List<String> getRoles(UserModel user) {
        return user.getRoleMappingsStream().map(RoleModel::getName).collect(Collectors.toList());
    }

    private List<CredentialRepresentation> getCredentials(UserModel user) {
        auth.users().requireManage(user);

        // Remove secret data from credentials
        List<CredentialRepresentation> models = user.credentialManager().getStoredCredentialsStream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        models.forEach(c -> c.setSecretData(null));
        return models;
    }
}
