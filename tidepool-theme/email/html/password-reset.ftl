<#import "template.ftl" as layout>
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
        Hey there!
    <#elseif section = "content">
        You requested a password reset. If you didn't request this, please ignore this email.<br /><br />Otherwise, click the link below. The link will expire in ${kcSanitize(linkExpirationFormatter(linkExpiration))?no_esc}.
    <#elseif section = "actionText">
        Reset Password
    <#elseif section = "actionLink">${kcSanitize(link)?no_esc}</#if>
</@layout.emailLayout>
