<#import "template.ftl" as layout>
<#-- Tidepool addition (not in legacy theme; added in Keycloak 26). Informational only. -->
<@layout.emailLayout displayHeader=false displayAction=false; section>
    <#if section = "content">
        ${kcSanitize(msg("eventUserDisabledByTemporaryLockoutHtml", event.date))?no_esc}
    </#if>
</@layout.emailLayout>
