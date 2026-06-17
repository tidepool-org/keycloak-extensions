<#import "template.ftl" as layout>
<#-- Tidepool addition: sent to a user's PREVIOUS email address when their account email changes, -->
<#-- so the original mailbox owner can detect an unexpected change. Informational only — no CTA. -->
<#-- Attributes are supplied by EmailChangedNotificationEventListenerProvider: newEmail, changedAt -->
<#-- (ISO-8601 UTC), and optionally ipAddress. Body copy inlined in English to match the other -->
<#-- Tidepool emails. -->
<@layout.emailLayout displayHeader=true displayAction=false; section>
    <#if section = "header">
        Hey there!
    <#elseif section = "content">
        The email address for your Tidepool account was changed to ${newEmail?html}.<br /><br />This change was made on ${changedAt?html}<#if ipAddress?has_content> from IP address ${ipAddress?html}</#if>.<br /><br />If you made this change, no action is needed. If you did not request it, please contact Tidepool Support right away.
    </#if>
</@layout.emailLayout>
