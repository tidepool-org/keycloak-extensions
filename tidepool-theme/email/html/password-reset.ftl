<#import "template.ftl" as layout>
<#-- Tidepool change: rewritten to use the new sectioned emailLayout (header / content / actionText / actionLink). -->
<#-- Upstream emits a single ${msg("passwordResetBodyHtml", ...)} blob; Tidepool splits the message so the action -->
<#-- becomes a styled CTA button rendered by the layout, and the body copy is inlined here in English -->
<#-- (Tidepool currently ships English-only login/email content). -->
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
        Hey there!
    <#elseif section = "content">
        You requested a password reset. If you didn't request this, please ignore this email.<br /><br />Otherwise, click the link below. The link will expire in ${kcSanitize(linkExpirationFormatter(linkExpiration))?no_esc}.
    <#elseif section = "actionText">
        Reset Password
    <#elseif section = "actionLink">${kcSanitize(link)?no_esc}</#if>
</@layout.emailLayout>
