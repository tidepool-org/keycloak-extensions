<#import "template.ftl" as layout>
<#-- Tidepool addition (not in legacy theme; added in Keycloak 26 to replace event-remove_totp -->
<#-- for arbitrary credential types). Informational only — no header, no CTA. -->
<@layout.emailLayout displayHeader=false displayAction=false; section>
    <#if section = "content">
        ${kcSanitize(msg("eventRemoveCredentialBodyHtml", event.getDetail("credential_type")!"unknown", event.date, event.ipAddress))?no_esc}
    </#if>
</@layout.emailLayout>
