<#import "template.ftl" as layout>
<#-- Tidepool change: displayMessage=false suppresses the page-level message block; the instructions below -->
<#-- carry the same information in a tidier layout. -->
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("emailLinkIdpTitle", idpDisplayName)}
    <#elseif section = "form">
            <div class="${properties.kcFormGroupClass!}">
                <#-- Tidepool change: pass brokerContext.email (was brokerContext.username) so the instruction text -->
                <#-- references the email the verification was sent to, which matches what users see on the disabled -->
                <#-- input below. -->
                <p id="instruction1" class="instruction">
                    ${msg("emailLinkIdp1", idpDisplayName, brokerContext.email, realm.displayName)}
                </p>
                <#-- Tidepool addition: show the email address as a disabled input so users have a visual confirmation -->
                <#-- of which address the verification link was sent to (and can spot typos). -->
                <div class="attempted-username instruction">
                    <input disabled
                           id="username"
                           class="${properties.kcInputClass!}"
                           type="text" autocomplete="off"
                           value="${brokerContext.email}"
                    />
                </div>
                <#-- Tidepool change: promote "resend verification" from a small inline link to a primary button -->
                <#-- (replaces the upstream "click here ... to re-send the email" instruction). Same loginAction URL, -->
                <#-- just a more discoverable CTA. -->
                <p id="instruction2" class="instruction">
                    <a class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} tp-btn-fw"  href="${url.loginAction}">${msg("emailLinkIdpResendVerificationCode")}</a>
                </p>
                <p id="instruction3" class="instruction">
                    ${msg("emailLinkIdp4")} <a href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailLinkIdp5")}
                </p>
            </div>
    </#if>
</@layout.registrationLayout>
