<#import "template.ftl" as layout>
<#-- Upstream renders two inline "click here" links in a prose paragraph. We -->
<#-- replace them with stacked block buttons — the first (restart) gets primary -->
<#-- styling at the top, the second (continue) gets the secondary style below. -->
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("pageExpiredTitle")}
    <#elseif section = "form">
        <div class="tp-page-expired-actions">
            <a id="loginRestartLink"
               href="${url.loginRestartFlowUrl}"
               class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}">${msg("pageExpiredRestart")}</a>
            <a id="loginContinueLink"
               href="${url.loginAction}"
               class="${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!}">${msg("pageExpiredContinue")}</a>
        </div>
    </#if>
</@layout.registrationLayout>
