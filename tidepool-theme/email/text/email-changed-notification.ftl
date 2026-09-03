<#ftl output_format="plainText">
<#-- Plain-text counterpart of html/email-changed-notification.ftl. Keycloak renders BOTH a text and -->
<#-- an html body for every email, so this must exist or the send fails with TemplateNotFoundException. -->
<#-- EmailChangedNotificationEventListenerProvider supplies newEmail (plus changedAt and ipAddress, which -->
<#-- are deliberately not shown). -->
Hey there!

The email address for your Tidepool account was changed to ${newEmail}.

If you made this change, no action is needed. If you did not request it, please contact Tidepool Support right away.
