package org.tidepool.keycloak.extensions.terms;

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
import java.util.stream.Collectors;

@AutoService(RequiredActionFactory.class)
public class TidepoolTermsRequiredAction implements RequiredActionProvider, RequiredActionFactory {

    public static final String PROVIDER_ID = "tidepool_terms_required_action";

    // Changing the attribute will break compatibility with the user migration plugin
    public static final String TERMS_ATTRIBUTE = "terms_and_conditions";

    public static final Map<String, String> FORMS = Map.of(
            "patient","patient_terms.ftl",
            "clinic", "clinician_terms.ftl",
            "clinician", "clinician_terms.ftl"
    );

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
        if (userHasAcceptedTerms(context)) {
            context.getUser().removeRequiredAction(PROVIDER_ID);
        } else {
            context.getUser().addRequiredAction(PROVIDER_ID);
        }
    }

    private RoleModel getRole(RequiredActionContext context) {
        Set<String> accountTypes = FORMS.keySet();
        List<RoleModel> roles = context
                .getUser()
                .getRoleMappingsStream()
                .filter(r -> accountTypes.contains(r.getName()))
                .collect(Collectors.toList());

        if (roles.size() == 0) {
            throw new IllegalStateException("User has no roles");
        } else if (roles.size() > 1) {
            throw new IllegalStateException("User has more than one role");
        }

        return roles.get(0);
    }

    private boolean userHasAcceptedTerms(RequiredActionContext context) {
        String attr = context.getUser().getFirstAttribute(TERMS_ATTRIBUTE);
        try {
            long ts = Long.parseLong(attr);
            return ts > 0;
        } catch (NumberFormatException e) {
            context.getUser().removeAttribute(TERMS_ATTRIBUTE);
            return false;
        }
    }


    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        String form = FORMS.get(getRole(context).getName());
        Response challenge = context.form().createForm(form);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        if (!context.getHttpRequest().getDecodedFormParameters().containsKey("accept")) {
            context.getUser().removeAttribute(TERMS_ATTRIBUTE);
            context.failure();
            return;
        }

        Long secondsSinceEpoch = java.time.Instant.now().getEpochSecond();
        context.getUser().setAttribute(TERMS_ATTRIBUTE, List.of(String.valueOf(secondsSinceEpoch)));
        context.success();
    }

    @Override
    public String getDisplayText() {
        return "Tidepool: Terms and Conditions";
    }

    @Override
    public void close() {
        // NOOP
    }
}
