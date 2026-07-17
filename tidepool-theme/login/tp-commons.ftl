<#-- Shared Tidepool login snippets reused across several templates. -->
<#-- Import with: <#import "tp-commons.ftl" as tp> then call e.g. <@tp.checkIcon/>. -->

<#-- Selected-state checkmark for the radio-card pickers (role / authenticator / -->
<#-- IDP selection). Rendered inside .tp-card-option-check. -->
<#macro checkIcon>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
</#macro>

<#-- True when the current page renders the error from an expired action-token -->
<#-- link (password reset, email verification, update-email confirmation). -->
<#-- Keycloak surfaces the expiry through different message keys depending on -->
<#-- whether an auth session exists (flow restart uses ...SessionExists, the -->
<#-- no-session error page uses ...NoSession, and required-action expiry uses -->
<#-- expiredActionMessage) — treat them all as the same "link expired" state. -->
<#function isActionLinkExpired>
    <#if !(message??) || message.type != 'error'>
        <#return false>
    </#if>
    <#local summary = message.summary?trim>
    <#return summary == msg("expiredActionMessage")?trim
        || summary == msg("expiredActionTokenNoSessionMessage")?trim
        || summary == msg("expiredActionTokenSessionExistsMessage")?trim>
</#function>

<#-- The dedicated link-expired card body (Figma node 14515:80071): blue-60 -->
<#-- prose plus a full-width primary "Take me back to the application" button. -->
<#-- Falls back to restarting the login flow when the client has no base URL. -->
<#macro linkExpiredBody>
    <div id="tp-link-expired">
        <p class="tp-link-expired-message">${msg("linkExpiredMessage")}</p>
        <#if client?? && client.baseUrl?has_content>
            <#local backUrl = client.baseUrl>
        <#else>
            <#local backUrl = url.loginRestartFlowUrl>
        </#if>
        <a id="backToApplication" href="${backUrl}" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} tp-back-to-app">${msg("backToApplication")}</a>
    </div>
</#macro>

<#-- "Having trouble logging in? / Important:" disclaimer at the bottom of the OTP, -->
<#-- recovery-code, authenticator-select, and IDP-select screens. The second -->
<#-- ("Important:") paragraph is omitted on the IDP-select screen (showImportant=false). -->
<#macro otpDisclaimer showImportant=true>
    <div class="tp-otp-disclaimer">
        <p>
            <strong>${msg("loginOtpHelpPromptStrong")}</strong>
            ${msg("loginOtpHelpPrompt")}
            <a href="http://support.tidepool.org/" target="_blank" rel="noreferrer noopener">${msg("loginOtpHelpLink")}</a>
        </p>
        <#if showImportant>
        <p>
            <strong>${msg("loginOtpImportantStrong")}</strong>
            ${msg("loginOtpImportant")}
        </p>
        </#if>
    </div>
</#macro>
