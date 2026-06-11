package org.tidepool.keycloak.extensions.resource;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.tidepool.keycloak.extensions.activity.UserActivityRecorder;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TidepoolAdminResource extends AdminResource {

    private static final Logger LOG = Logger.getLogger(TidepoolAdminResource.class);

    private static final String ID_SEPARATOR = ",";
    private static final int BACKFILL_BATCH_SIZE = 100;

    private final KeycloakSession session;

    public TidepoolAdminResource(KeycloakSession session) {
        super(session);
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

    /**
     * One-off backfill: writes an {@code IDP_LINKS_CHANGED} user-activity row for every user in this realm
     * that currently has at least one linked identity provider, seeding the outbox with existing links.
     * Idempotent — re-running just re-records each user's current link set (consumers upsert).
     *
     * <p>Runs in batches of {@value #BACKFILL_BATCH_SIZE}, each in its own transaction, so the
     * persistence context stays bounded and a failure only loses its own batch (counted in
     * {@code failed}) instead of rolling back the whole run. Users whose links disappeared since the
     * id query are counted in {@code skipped}.
     */
    @POST
    @Path("backfill-idp-links")
    @Produces({MediaType.APPLICATION_JSON})
    public Response backfillIdpLinks() {
        RealmModel realm = session.getContext().getRealm();
        // Authorize against the realm being written to, not the admin realm the token was issued by.
        authFor(realm).realm().requireManageRealm();

        String realmId = realm.getId();
        long now = Time.currentTimeMillis();

        // Only the users that actually have a federated link, read from Keycloak's FEDERATED_IDENTITY table.
        @SuppressWarnings({"unchecked", "resource"})
        List<String> userIds = session.getProvider(JpaConnectionProvider.class).getEntityManager()
                .createNativeQuery("SELECT DISTINCT user_id FROM federated_identity WHERE realm_id = :realmId")
                .setParameter("realmId", realmId)
                .getResultList();

        int backfilled = 0;
        int skipped = 0;
        int failed = 0;
        KeycloakSessionFactory factory = session.getKeycloakSessionFactory();
        for (int start = 0; start < userIds.size(); start += BACKFILL_BATCH_SIZE) {
            List<String> batch = userIds.subList(start, Math.min(start + BACKFILL_BATCH_SIZE, userIds.size()));
            int[] counts = new int[2]; // [recorded, skipped]
            try {
                KeycloakModelUtils.runJobInTransaction(factory, batchSession -> {
                    RealmModel batchRealm = batchSession.realms().getRealm(realmId);
                    // Fresh sessions have no realm in their context; user lookups require one.
                    batchSession.getContext().setRealm(batchRealm);
                    UserActivityRecorder recorder = new UserActivityRecorder(batchSession);
                    for (String userId : batch) {
                        UserModel user = batchSession.users().getUserById(batchRealm, userId);
                        // Skip users (or links) removed since the id query; nothing to seed for them.
                        if (user != null && recorder.recordIdpLinksIfPresent(batchRealm, user, now)) {
                            counts[0]++;
                        } else {
                            counts[1]++;
                        }
                    }
                });
                backfilled += counts[0];
                skipped += counts[1];
            } catch (RuntimeException e) {
                failed += batch.size();
                LOG.errorf(e, "IdP-links backfill batch failed for realm %s (%d user(s) not backfilled)",
                        realm.getName(), batch.size());
            }
        }

        return Response.ok(Map.of(
                "realm", realm.getName(),
                "backfilled", backfilled,
                "skipped", skipped,
                "failed", failed)).build();
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
