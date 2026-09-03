<#import "template.ftl" as layout>
<#-- Tidepool addition: sent to a user's PREVIOUS email address when their account email changes, -->
<#-- so the original mailbox owner can detect an unexpected change. Informational only — no CTA. -->
<#-- EmailChangedNotificationEventListenerProvider supplies newEmail (plus changedAt and ipAddress, -->
<#-- which are deliberately not shown). Body copy inlined in English to match the other Tidepool -->
<#-- emails. -->
<@layout.emailLayout displayHeader=true displayAction=false; section>
    <#if section = "header">
        Hey there!
    <#elseif section = "content">
        The email address for your Tidepool account was changed to ${newEmail}.<br /><br />If you made this change, no action is needed. If you did not request it, please contact Tidepool Support right away.
    </#if>
</@layout.emailLayout>
