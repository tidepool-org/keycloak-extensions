<#-- Shared Tidepool login snippets reused across several templates. -->
<#-- Import with: <#import "tp-commons.ftl" as tp> then call e.g. <@tp.checkIcon/>. -->

<#-- Selected-state checkmark for the radio-card pickers (role / authenticator / -->
<#-- IDP selection). Rendered inside .tp-card-option-check. -->
<#macro checkIcon>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
</#macro>

<#-- True when the current page renders the error from an action-token link -->
<#-- (password reset, email verification, update-email confirmation) that can no -->
<#-- longer be used. Keycloak surfaces that through several message keys: -->
<#--  - expiry: expiredActionTokenSessionExistsMessage (flow restart), -->
<#--    expiredActionTokenNoSessionMessage (sessionless error page) and -->
<#--    expiredActionMessage (required-action expiry); -->
<#--  - emailVerificationCancelled: UpdateEmailActionTokenHandler when the -->
<#--    pending email the token was issued for is gone, i.e. the user cancelled -->
<#--    the change (CancelableUpdateEmail drops kc.email.pending) and then -->
<#--    opened the confirmation link anyway; -->
<#--  - staleEmailVerificationLink: the email the token was issued against no -->
<#--    longer matches the account, i.e. the change was already confirmed and -->
<#--    the link is being opened a second time. -->
<#-- Treat them all as the same "link no longer active" state. -->
<#function isActionLinkExpired>
    <#if !(message??) || message.type != 'error'>
        <#return false>
    </#if>
    <#local summary = message.summary?trim>
    <#return summary == msg("expiredActionMessage")?trim
        || summary == msg("expiredActionTokenNoSessionMessage")?trim
        || summary == msg("expiredActionTokenSessionExistsMessage")?trim
        || summary == msg("emailVerificationCancelled")?trim
        || summary == msg("staleEmailVerificationLink")?trim>
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
