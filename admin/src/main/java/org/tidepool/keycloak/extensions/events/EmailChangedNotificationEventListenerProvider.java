package org.tidepool.keycloak.extensions.events;

import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Notifies a user's <em>previous</em> email address whenever their account email changes, so the
 * original mailbox owner can detect an unexpected change (e.g. an account takeover).
 *
 * <p>Keycloak fires {@link EventType#UPDATE_EMAIL} (the self-service update-email required action)
 * and the legacy {@link EventType#UPDATE_PROFILE} <em>after</em> the new address is persisted. Both
 * carry {@link Details#PREVIOUS_EMAIL} / {@link Details#UPDATED_EMAIL} details, but only when the
 * email actually changed &mdash; so the presence of a previous email is our change signal, and it is
 * the address we notify.
 *
 * <p>Because the change is already committed by the time the event fires, {@link UserModel#getEmail()}
 * returns the <em>new</em> address; {@link EmailTemplateProvider} delivers to whatever that getter
 * returns. We therefore wrap the user in a {@link UserModelDelegate} whose {@code getEmail()} returns
 * the previous address, redirecting delivery to the old mailbox.
 *
 * <p>Admin-initiated email changes are intentionally out of scope: an {@link AdminEvent} exposes only
 * the new email, not the previous address we would need to notify.
 */
public class EmailChangedNotificationEventListenerProvider implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(EmailChangedNotificationEventListenerProvider.class);

    static final String SUBJECT_MESSAGE_KEY = "emailChangedNotificationSubject";
    static final String TEMPLATE_NAME = "email-changed-notification.ftl";

    private final KeycloakSession session;

    public EmailChangedNotificationEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (event == null) {
            return;
        }
        EventType type = event.getType();
        if (type != EventType.UPDATE_EMAIL && type != EventType.UPDATE_PROFILE) {
            return;
        }

        Map<String, String> details = event.getDetails();
        if (details == null) {
            return;
        }
        String previousEmail = details.get(Details.PREVIOUS_EMAIL);
        String updatedEmail = details.get(Details.UPDATED_EMAIL);

        // previous_email / updated_email are only set when the email actually changed, so a present
        // previous address is the change signal. Guard against blank/unchanged values regardless.
        if (previousEmail == null || previousEmail.isBlank()) {
            return;
        }
        if (updatedEmail != null && previousEmail.equalsIgnoreCase(updatedEmail)) {
            return;
        }

        sendNotification(event, previousEmail, updatedEmail);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Admin-initiated email changes are intentionally out of scope; the AdminEvent does not
        // expose the previous email address required to notify the old mailbox.
    }

    private void sendNotification(Event event, String previousEmail, String updatedEmail) {
        String realmId = event.getRealmId();
        String userId = event.getUserId();
        if (realmId == null || userId == null) {
            return;
        }

        RealmModel realm = session.realms().getRealm(realmId);
        if (realm == null) {
            return;
        }

        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            return;
        }

        // The user's email is already the NEW address; redirect delivery to the previous address.
        UserModel previousEmailRecipient = new UserModelDelegate(user) {
            @Override
            public String getEmail() {
                return previousEmail;
            }
        };

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("newEmail", updatedEmail != null ? updatedEmail : user.getEmail());
        attributes.put("changedAt", Instant.ofEpochMilli(event.getTime()).truncatedTo(ChronoUnit.SECONDS).toString());
        if (event.getIpAddress() != null && !event.getIpAddress().isBlank()) {
            attributes.put("ipAddress", event.getIpAddress());
        }

        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(previousEmailRecipient)
                    .send(SUBJECT_MESSAGE_KEY, TEMPLATE_NAME, attributes);
            LOG.infof("Sent email-change notification to the previous address of user %s in realm %s",
                    userId, realm.getName());
        } catch (EmailException e) {
            LOG.warnf(e, "Failed to send email-change notification to the previous address of user %s in realm %s",
                    userId, realm.getName());
        }
    }

    @Override
    public void close() {
    }
}
