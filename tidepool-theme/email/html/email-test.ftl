<#import "template.ftl" as layout>
<#-- Tidepool addition (not in legacy theme; added in Keycloak 26). Informational SMTP test, -->
<#-- no header / no CTA. Body copy uses the upstream msg() key so it stays in sync. -->
<@layout.emailLayout displayHeader=false displayAction=false; section>
    <#if section = "content">
        ${kcSanitize(msg("emailTestBodyHtml",realmName))?no_esc}
    </#if>
</@layout.emailLayout>
