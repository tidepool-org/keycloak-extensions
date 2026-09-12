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
 * <p>This subclass surfaces a Tidepool cancel button whenever the change can be safely abandoned:
 * either the action is an enforced AIA, or an unverified {@link UserModel#EMAIL_PENDING pending
 * email} change exists (so the user still has a current, verified email to fall back on).
 * Cancelling discards the pending email and removes the {@code UPDATE_EMAIL} required action, so the
 * account keeps its current email. (With email verification enabled the primary email is never
 * changed until the new address is verified, so there is nothing to roll back &mdash; only the
 * pending change is dropped.)
 *
 * <p>The cancel path is gated on {@link #isCancelAllowed(RequiredActionContext)} both when rendering
 * and when handling the submit. A plain {@code UPDATE_EMAIL} required action forced on login with no
 * pending change yet therefore cannot be skipped &mdash; there must be a pending change to abandon
 * (or an enforced AIA).
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
        if (isCancelAllowed(context)) {
            // Standard cancel-aia is unavailable here (enforced AIA, or a pending required action on
            // login); expose our own. Set before delegating: the LoginFormsProvider is cached per
            // session, so the attribute persists into the form super renders.
            context.form().setAttribute(CANCEL_ALLOWED_ATTRIBUTE, true);
        }
        super.requiredActionChallenge(context);
    }

    @Override
    public void processAction(RequiredActionContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey(CANCEL_PARAMETER) && isCancelAllowed(context)) {
            UserModel user = context.getUser();
            // Drop the unverified pending email and clear the action; the current email stands.
            user.removeAttribute(UserModel.EMAIL_PENDING);
            user.removeRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL);
            LOG.debugf("User %s cancelled the update-email action; keeping the current email",
                    user.getId());
            context.success();
            return;
        }
        // A successful submit re-renders the "verification email sent" confirmation form from inside
        // super.processAction (via sendEmailUpdateConfirmation), bypassing requiredActionChallenge.
        // Expose the cancel button on that form too. This only governs button visibility; the cancel
        // action above stays gated on isCancelAllowed, so a mandatory update still cannot be skipped.
        if (isCancelAllowed(context) || isEmailSubmission(formData)) {
            context.form().setAttribute(CANCEL_ALLOWED_ATTRIBUTE, true);
        }
        super.processAction(context);
    }

    /** A non-blank email value is being submitted (i.e. a change that will land on the confirmation form). */
    static boolean isEmailSubmission(MultivaluedMap<String, String> formData) {
        String email = formData.getFirst(UserModel.EMAIL);
        return email != null && !email.isBlank();
    }

    /**
     * Whether the user may cancel the email update: when the action is an enforced AIA (the standard
     * cancel-aia button is suppressed) or when an unverified pending email change already exists. In
     * both cases the user has a current verified email to keep, so abandoning the change is safe.
     */
    static boolean isCancelAllowed(RequiredActionContext context) {
        return isEnforcedAppInitiatedAction(context) || hasPendingEmail(context);
    }

    private static boolean hasPendingEmail(RequiredActionContext context) {
        UserModel user = context.getUser();
        return user != null && user.getFirstAttribute(UserModel.EMAIL_PENDING) != null;
    }

    static boolean isEnforcedAppInitiatedAction(RequiredActionContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        if (authSession == null) {
            return false;
        }
        return Boolean.TRUE.toString().equals(authSession.getClientNote(Constants.KC_ACTION_ENFORCED));
    }
}
