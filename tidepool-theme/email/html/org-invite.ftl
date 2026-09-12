<#import "template.ftl" as layout>
<#-- Tidepool addition (not in legacy theme; added in Keycloak 26 for the Organizations feature). -->
<#-- Action template with CTA — body copy inlined in English to match the other Tidepool action emails. -->
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
        <#if firstName?? && lastName??>
            Hi ${firstName} ${lastName}!
        <#else>
            Hey there!
        </#if>
    <#elseif section = "content">
        You were invited to join the ${organization.name} organization. Click the link below to join. The link will expire in ${kcSanitize(linkExpirationFormatter(linkExpiration))?no_esc}.<br /><br />If you don't want to join the organization, just ignore this message.
    <#elseif section = "actionText">
        Join Organization
    <#elseif section = "actionLink">${kcSanitize(link)?no_esc}</#if>
</@layout.emailLayout>
