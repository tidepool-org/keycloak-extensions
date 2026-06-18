<#ftl output_format="plainText">
<#-- Plain-text counterpart of html/email-changed-notification.ftl. Keycloak renders BOTH a text and -->
<#-- an html body for every email, so this must exist or the send fails with TemplateNotFoundException. -->
<#-- Attributes supplied by EmailChangedNotificationEventListenerProvider: newEmail, changedAt (ISO-8601 -->
<#-- UTC), and optionally ipAddress. -->
Hey there!

The email address for your Tidepool account was changed to ${newEmail}.

This change was made on ${changedAt}<#if ipAddress?has_content> from IP address ${ipAddress}</#if>.

If you made this change, no action is needed. If you did not request it, please contact Tidepool Support right away.
