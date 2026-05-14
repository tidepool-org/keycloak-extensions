<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "passkeys.ftl" as passkeys>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password'); section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <form id="kc-form-login" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                    <@field.password name="password" label=msg("password") forgotPassword=realm.resetPasswordAllowed autofocus=true autocomplete="current-password" />

                    <div class="${properties.kcFormGroupClass!}">
                        <div class="${properties.kcFormActionGroupClass!}">
                            <button class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" name="login" id="kc-login" type="submit">${msg("next")}</button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
        <@passkeys.conditionalUIData />
    <#elseif section = "info">
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
