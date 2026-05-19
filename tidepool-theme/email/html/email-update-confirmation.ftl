<#import "template.ftl" as layout>
<#-- Tidepool addition (not in legacy theme; added in Keycloak 26 for the new "update email" -->
<#-- required-action flow). Action template with CTA — body copy inlined in English to match -->
<#-- the other Tidepool action emails (password-reset / email-verification). -->
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
        Hey there!
    <#elseif section = "content">
        To update your Tidepool account with the email address ${newEmail}, click the link below. The link will expire in ${kcSanitize(linkExpirationFormatter(linkExpiration))?no_esc}.<br /><br />If you didn't request this, please ignore this email.
    <#elseif section = "actionText">
        Verify New Email
    <#elseif section = "actionLink">${kcSanitize(link)?no_esc}</#if>
</@layout.emailLayout>
