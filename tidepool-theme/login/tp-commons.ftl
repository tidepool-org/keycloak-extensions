<#-- Shared Tidepool login snippets reused across several templates. -->
<#-- Import with: <#import "tp-commons.ftl" as tp> then call e.g. <@tp.checkIcon/>. -->

<#-- Selected-state checkmark for the radio-card pickers (role / authenticator / -->
<#-- IDP selection). Rendered inside .tp-card-option-check. -->
<#macro checkIcon>
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
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
