<#import "template.ftl" as layout>
<@layout.emailLayout displayHeader=false displayAction=false; section>
    <#if section = "content">
        ${kcSanitize(msg("eventUpdatePasswordBodyHtml",event.date, event.ipAddress))?no_esc}
    </#if>
</@layout.emailLayout>
