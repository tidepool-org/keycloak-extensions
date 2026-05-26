<#import "template.ftl" as layout>
<#-- Tidepool override: the base template embeds the broker account id in the prose -->
<#-- and offers two inline "click here" links (re-send / continue). We keep the -->
<#-- page's warning banner, explain that the email check exists because the user is -->
<#-- linking accounts, show the address the verification was sent to as a read-only -->
<#-- field, and replace the inline links with stacked block buttons — "Continue" -->
<#-- (primary) over "Resend email" (secondary). Both post to url.loginAction; the -->
<#-- authenticator proceeds if the email is already verified (e.g. the link was -->
<#-- opened in another browser), otherwise it re-sends the verification email. -->
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("emailLinkIdpTitle", idpDisplayName)}
    <#elseif section = "form">
        <p id="instruction1" class="instruction">
            ${msg("emailLinkIdp1", idpDisplayName, brokerContext.email, realm.displayName)}
        </p>
        <div class="tp-attempted-username">
            <span class="tp-attempted-username-value">${brokerContext.email}</span>
        </div>
        <p id="instruction2" class="instruction">
            ${msg("emailLinkIdpHelp")}
        </p>
        <div class="tp-page-expired-actions">
            <a id="continueLink"
               href="${url.loginAction}"
               class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}">${msg("emailLinkIdpContinue")}</a>
            <a id="resendEmailLink"
               href="${url.loginAction}"
               class="${properties.kcButtonDefaultClass!} ${properties.kcButtonBlockClass!}">${msg("emailLinkIdpResendVerificationCode")}</a>
        </div>
    </#if>
</@layout.registrationLayout>
