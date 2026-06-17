package org.tidepool.keycloak.extensions.requiredactions;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import org.jboss.logging.Logger;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.requiredactions.UpdateEmail;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Overrides the built-in {@code UPDATE_EMAIL} required action to offer a cancel button when the
 * action is an <em>enforced</em> application-initiated action (AIA).
 *
 * <p>Keycloak hides the standard {@code cancel-aia} button whenever a requested
 * {@code kc_action=UPDATE_EMAIL} also matches a pending required action on the user: the auth
 * session note {@link Constants#KC_ACTION_ENFORCED} is set, and
 * {@code FreeMarkerLoginFormsProvider} consequently omits the {@code isAppInitiatedAction} flag the
 * template keys off. The user is then stuck on the update-email form with no way out.
 *
 * <p>This subclass surfaces a Tidepool cancel button in exactly that case. Cancelling discards any
 * unverified {@link UserModel#EMAIL_PENDING pending email} and removes the {@code UPDATE_EMAIL}
 * required action, so the account keeps its current email. (With email verification enabled the
 * primary email is never changed until the new address is verified, so there is nothing to roll
 * back &mdash; only the pending change is dropped.)
 *
 * <p>The cancel path is gated on {@link #isEnforcedAppInitiatedAction(RequiredActionContext)} both
 * when rendering and when handling the submit, so it cannot be used to skip an {@code UPDATE_EMAIL}
 * action that was forced purely on login (no AIA).
 *
 * <p>Registered with {@code getId() == "UPDATE_EMAIL"} (inherited), it replaces the built-in
 * provider for that id &mdash; Keycloak's factory loading is last-write-wins and module providers
 * load after built-ins.
 */
@AutoService(RequiredActionFactory.class)
public class CancelableUpdateEmail extends UpdateEmail {

    private static final Logger LOG = Logger.getLogger(CancelableUpdateEmail.class);

    /** Form attribute consumed by {@code update-email.ftl} to render the cancel button. */
    static final String CANCEL_ALLOWED_ATTRIBUTE = "updateEmailCancelAllowed";

    /** Submit-button name posted when the user cancels an enforced email update. */
    static final String CANCEL_PARAMETER = "cancel-update-email";

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        if (isEnforcedAppInitiatedAction(context)) {
            // The standard cancel-aia button is suppressed for enforced AIAs; expose our own.
            // Set before delegating: the LoginFormsProvider is cached per session, so the attribute
            // persists into the form super renders.
            context.form().setAttribute(CANCEL_ALLOWED_ATTRIBUTE, true);
        }
        super.requiredActionChallenge(context);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey(CANCEL_PARAMETER) && isEnforcedAppInitiatedAction(context)) {
            UserModel user = context.getUser();
            // Drop the unverified pending email and clear the action; the current email stands.
            user.removeAttribute(UserModel.EMAIL_PENDING);
            user.removeRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL);
            LOG.debugf("User %s cancelled an enforced update-email action; keeping the current email",
                    user.getId());
            context.success();
            return;
        }
        super.processAction(context);
    }

    static boolean isEnforcedAppInitiatedAction(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        if (authSession == null) {
            return false;
        }
        return Boolean.TRUE.toString().equals(authSession.getClientNote(Constants.KC_ACTION_ENFORCED));
    }
}
