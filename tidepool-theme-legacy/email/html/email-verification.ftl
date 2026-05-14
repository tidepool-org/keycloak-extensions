<#import "template.ftl" as layout>
<#-- Tidepool change: switched to the sectioned emailLayout. Upstream emits a single ${msg("emailVerificationBodyHtml", ...)} -->
<#-- blob; here we hand the link to the layout's actionLink slot so it renders as a Tidepool CTA button, and the body copy -->
<#-- is inlined in English. -->
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
        Hey there!
    <#elseif section = "content">
        Please verify your Tidepool account! The verification link will expire in ${kcSanitize(linkExpirationFormatter(linkExpiration))?no_esc}.
    <#elseif section = "actionText">
        Verify Your Account
    <#elseif section = "actionLink">${kcSanitize(link)?no_esc}</#if>
</@layout.emailLayout>
