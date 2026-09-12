<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
            ${kcSanitize(msg("${messageHeader}"))?no_esc}
        <#else>
            ${message.summary}
        </#if>
    <#elseif section = "form">
        <div id="kc-info-message">
            <#-- Sanitized so messages can carry markup (e.g. the bolded address in
                 emailUpdateConfirmationSent); kcSanitize strips anything unsafe. -->
            <p class="instruction">${kcSanitize(message.summary)?no_esc}<#if requiredActions??><#list requiredActions>: <b><#items as reqActionItem>${kcSanitize(msg("requiredAction.${reqActionItem}"))?no_esc}<#sep>, </#items></b></#list><#else></#if></p>
            <#if !skipLink??>
                <#if pageRedirectUri?has_content>
                    <a href="${pageRedirectUri}" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} tp-back-to-app">${msg("backToApplication")}</a>
                <#elseif actionUri?has_content>
                    <a href="${actionUri}" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} tp-back-to-app">${msg("proceedWithAction")}</a>
                <#elseif (client.baseUrl)?has_content>
                    <a href="${client.baseUrl}" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} tp-back-to-app">${msg("backToApplication")}</a>
                </#if>
            </#if>
        </div>
    </#if>
</@layout.registrationLayout>
