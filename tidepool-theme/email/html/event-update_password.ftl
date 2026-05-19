<#import "template.ftl" as layout>
<#-- Tidepool change: switched to the sectioned emailLayout; informational only (no header, no CTA). -->
<@layout.emailLayout displayHeader=false displayAction=false; section>
    <#if section = "content">
        ${kcSanitize(msg("eventUpdatePasswordBodyHtml",event.date, event.ipAddress))?no_esc}
    </#if>
</@layout.emailLayout>
