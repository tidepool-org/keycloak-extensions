<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('recoveryCodeInput') subtitle=msg("loginRecoverySubtitle"); section>
    <#if section = "header">
        ${msg("auth-recovery-code-header")}
    <#elseif section = "form">
        <form id="kc-recovery-code-login-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <@field.input name="recoveryCodeInput" label=msg("auth-recovery-code-prompt", recoveryAuthnCodesInputBean.codeNumber?c) autofocus=true />

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcFormActionGroupClass!}">
                    <button class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" name="login" id="kc-login" type="submit">${msg("doLogIn")}</button>
                </div>
            </div>
        </form>

        <#-- Custom "Try another way" link rendered as an inline sentence to match -->
        <#-- the Figma design. Submits the standard tryAnotherWay=on POST so the -->
        <#-- flow continues with selectAuthenticator. -->
        <#if auth?has_content && auth.showTryAnotherWayLink()>
            <form id="tp-recovery-try-another-way" action="${url.loginAction}" method="post">
                <input type="hidden" name="tryAnotherWay" value="on"/>
                <p class="tp-otp-try-another-way">
                    ${msg("loginRecoveryTryAnotherWayPrefix")}
                    <a href="javascript:document.forms['tp-recovery-try-another-way'].requestSubmit()">${msg("loginOtpTryAnotherWay")}</a>
                </p>
            </form>
        </#if>

        <div class="tp-otp-disclaimer">
            <p>
                <strong>${msg("loginOtpHelpPromptStrong")}</strong>
                ${msg("loginOtpHelpPrompt")}
                <a href="http://support.tidepool.org/" target="_blank" rel="noreferrer noopener">${msg("loginOtpHelpLink")}</a>
            </p>
            <p>
                <strong>${msg("loginOtpImportantStrong")}</strong>
                ${msg("loginOtpImportant")}
            </p>
        </div>
    </#if>
</@layout.registrationLayout>
