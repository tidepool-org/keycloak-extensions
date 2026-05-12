<#import "template.ftl" as layout>
<#-- Tidepool change: displayMessage=false suppresses the page-level message block; this template renders -->
<#-- its own inline warning alert below so the messaging stays close to the disabled email field. -->
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("confirmLinkIdpTitle")}
    <#elseif section = "form">
        <#-- Tidepool addition: prominent warning alert explaining that the user is about to link the IdP-supplied -->
        <#-- account to an existing realm account with the same email. Replaces the upstream pair of plain buttons -->
        <#-- ("Review profile" / "Add to existing account") with a clearer single-action confirm. -->
        <div class="alert-warning ${properties.kcAlertClass!} pf-m-warning">
            <div class="pf-c-alert__icon">
                <span class="${properties.kcFeedbackWarningIcon!}"></span>
            </div>
            <span class="${properties.kcAlertTitleClass!}">${msg("emailLinkIdpConfirmEmailMessage", idpDisplayName, realm.displayName)}</span>
        </div>
        <form id="kc-register-form" action="${url.loginAction}" method="post">
            <#-- Tidepool change: show the email as a read-only/disabled input rather than letting the user edit it. -->
            <#-- Reason: linking must happen against the IdP-provided email; we explicitly do NOT want users to -->
            <#-- change the email at this step (which the upstream "Review profile" path would have allowed). -->
            <div class="${properties.kcFormGroupClass!}">
                <label for="username"
                       class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

                <div class="attempted-username">
                    <input disabled
                           id="username"
                           class="${properties.kcInputClass!}"
                           type="text" autocomplete="off"
                           value="${brokerContext.email}"
                    />
                </div>
            </div>

            <#-- Tidepool change: a single primary "Confirm" button replaces the upstream pair (review profile + -->
            <#-- link account). Posts submitAction=linkAccount, the same value used by the upstream "Add to existing -->
            <#-- account" path, so the server-side flow is unchanged. -->
            <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="submitAction" id="linkAccount" value="linkAccount">${msg("emailLinkIdpConfirm")}</button>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
