<#import "template.ftl" as layout>
<#import "tp-commons.ftl" as tp>
<#-- Expired action-token links opened without an auth session (e.g. in a -->
<#-- different browser) render here instead of restarting the login flow. -->
<#-- Show the same dedicated link-expired card as login-username.ftl (Figma -->
<#-- node 14515:80071). -->
<#assign actionLinkExpired = tp.isActionLinkExpired()>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if actionLinkExpired>${msg("linkExpiredTitle")}<#else>${kcSanitize(msg("errorTitle"))?no_esc}</#if>
    <#elseif section = "form">
        <div id="kc-error-message">
            <#if actionLinkExpired>
                <@tp.linkExpiredBody/>
            <#else>
                <p class="instruction">${kcSanitize(message.summary)?no_esc}</p>
                <#if traceId??>
                    <p class="instruction" id="traceId">${msg("traceIdSupportMessage", traceId)}</p>
                </#if>
                <#if !skipLink?? && client?? && client.baseUrl?has_content>
                    <a id="backToApplication" href="${client.baseUrl}" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} tp-back-to-app">${msg("backToApplication")}</a>
                </#if>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>
