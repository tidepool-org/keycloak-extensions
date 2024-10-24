package org.tidepool.keycloak.extensions.roles;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RoleModel;

import jakarta.ws.rs.core.Response;
import java.util.*;

@AutoService(RequiredActionFactory.class)
public class UserRolePromptRequiredAction implements RequiredActionProvider, RequiredActionFactory {

    public static final String PROVIDER_ID = "user_role_prompt_required_action";

    public static final Set<String> ROLES = new HashSet<>(Arrays.asList("patient", "clinic", "clinician"));

    public static final String ROLES_FORM_FTL = "user_role_prompt.ftl";

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }


    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (userHasRequiredRole(context)) {
            context.getUser().removeRequiredAction(PROVIDER_ID);
        } else {
            context.getUser().addRequiredAction(PROVIDER_ID);
        }
    }

    private boolean userHasRequiredRole(RequiredActionContext context) {
        return context.getUser().getRoleMappingsStream().anyMatch(
                r -> ROLES.contains(r.getName())
        );
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form().createForm(ROLES_FORM_FTL);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        String selectedRole = context.getHttpRequest().getDecodedFormParameters().getFirst("role");
        if (!ROLES.contains(selectedRole)) {
            context.failure();
            return;
        }

        RoleModel role = context.getRealm().getRole(selectedRole);
        if (role == null) {
            throw new IllegalStateException("couldn't find role" + selectedRole);
        }

        context.getUser().grantRole(role);
        context.success();
    }

    @Override
    public String getDisplayText() {
        return "Tidepool: User Role Prompt";
    }

    @Override
    public void close() {
        // NOOP
    }
}
