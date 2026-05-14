<#import "template.ftl" as layout>
<#-- Tidepool change: switched to the sectioned emailLayout. Event notifications carry no CTA and no headline -->
<#-- (just an informational body), so displayHeader/displayAction are both false. -->
<@layout.emailLayout displayHeader=false displayAction=false; section>
    <#if section = "content">
        ${kcSanitize(msg("eventLoginErrorBodyHtml",event.date,event.ipAddress))?no_esc}
    </#if>
</@layout.emailLayout>
