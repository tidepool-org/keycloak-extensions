<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#-- Tidepool override of the broker email-verification page (Figma file -->
<#-- g8xYrHViRt9nd1oXx0OuIF, node 13815:30239). The explanatory prose moves to the -->
<#-- title subtitle, a gold warning banner restates why verification is needed, the -->
<#-- address the link was sent to is shown as a labelled read-only field, and the -->
<#-- actions are a side-by-side Resend (secondary) / Next (primary) pair reusing the -->
<#-- shared .tp-totp-actions grid. Both actions GET url.loginAction; the authenticator -->
<#-- proceeds if the email is already verified (e.g. the link was opened in another -->
<#-- browser), otherwise it re-sends the verification email. -->
<@layout.registrationLayout displayMessage=false subtitle=msg("emailLinkIdp1", idpDisplayName); section>
    <#if section = "header">
        ${msg("emailLinkIdpTitle")}
    <#elseif section = "form">
        <div class="tp-warning-banner" role="alert">
            <span class="tp-warning-banner-icon" aria-hidden="true">
                <svg viewBox="0 0 18 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M8.13 1.5a1 1 0 0 1 1.74 0l7.36 12.75A1 1 0 0 1 16.36 16H1.64a1 1 0 0 1-.87-1.75L8.13 1.5zM9 6a.75.75 0 0 0-.75.75v3.5a.75.75 0 0 0 1.5 0v-3.5A.75.75 0 0 0 9 6zm0 7a1 1 0 1 0 0-2 1 1 0 0 0 0 2z"/></svg>
            </span>
            <div class="tp-warning-banner-text">
                <p class="tp-warning-banner-title">${msg("emailLinkIdpWarning", idpDisplayName)}</p>
            </div>
        </div>

        <div class="tp-idp-link-help">
            <p>${msg("emailLinkIdpHelp")}</p>
            <p>${kcSanitize(msg("emailLinkIdpHelp2"))?no_esc}</p>
        </div>

        <@field.group name="email" label=msg("email")>
            <div class="tp-attempted-username">
                <span class="tp-attempted-username-value">${brokerContext.email}</span>
            </div>
        </@field.group>

        <div class="tp-totp-actions">
            <a id="resendEmailLink"
               href="${url.loginAction}"
               class="${properties.kcButtonDefaultClass!}">${msg("emailLinkIdpResendVerificationCode")}</a>
            <a id="continueLink"
               href="${url.loginAction}"
               class="${properties.kcButtonPrimaryClass!}">${msg("emailLinkIdpContinue")}</a>
        </div>
    </#if>
</@layout.registrationLayout>
