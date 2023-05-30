<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("confirmLinkIdpTitle")}
    <#elseif section = "form">
        <div class="alert-warning ${properties.kcAlertClass!} pf-m-warning">
            <div class="pf-c-alert__icon">
                <span class="${properties.kcFeedbackWarningIcon!}"></span>
            </div>
            <span class="${properties.kcAlertTitleClass!}">${msg("emailLinkIdpConfirmEmailMessage", idpDisplayName, realm.displayName)}</span>
        </div>
        <form id="kc-register-form" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <label for="username"
                       class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

                <div class="attempted-username">
                    <input disabled
                           id="username"
                           class="${properties.kcInputClass!}"
                           type="text" autocomplete="off"
                           value="${brokerContext.modelUsername}"
                    />
                </div>
            </div>

            <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="submitAction" id="linkAccount" value="linkAccount">${msg("emailLinkIdpConfirm")}</button>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
