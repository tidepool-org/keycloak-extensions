<#import "template.ftl" as layout>
<#-- Tidepool addition (not in legacy theme; added in Keycloak 26 for the new "update email" -->
<#-- required-action flow). Action template with CTA — body copy inlined in English to match -->
<#-- the other Tidepool action emails. Copy from Figma node 14078:24446 (the password-reset -->
<#-- email screenshot with the update-email text overlaid); the subject lives in -->
<#-- messages_en.properties (emailUpdateConfirmationSubject). -->
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
        Hey there!
    <#elseif section = "content">
        You requested to change your email. Please confirm this change by clicking the link below.<br /><br />If you did not make this request, you can safely ignore this email.<br /><br />Otherwise, click the link below. The link will expire in ${kcSanitize(linkExpirationFormatter(linkExpiration))?no_esc}.
    <#elseif section = "actionText">
        Verify Email
    <#elseif section = "actionLink">${kcSanitize(link)?no_esc}</#if>
</@layout.emailLayout>
