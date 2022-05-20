<#import "template.ftl" as layout>
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
        Hey there!
    <#elseif section = "content">
        Please verify your Tidepool account! The verification link will expire in ${kcSanitize(linkExpirationFormatter(linkExpiration))?no_esc}.
    <#elseif section = "actionText">
        Verify Your Account
    <#elseif section = "actionLink">
        ${kcSanitize(link)?no_esc}
    </#if>
</@layout.emailLayout>
