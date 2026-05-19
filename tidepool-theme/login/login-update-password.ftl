<#import "template.ftl" as layout>
<#import "password-commons.ftl" as passwordCommons>
<#import "field.ftl" as field>
<#import "password-validation.ftl" as validator>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>
    <#if section = "header">
        ${msg("updatePasswordTitle")}
    <#elseif section = "form">
        <form id="kc-passwd-update-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" novalidate="novalidate">
            <@field.password name="password-new" label=msg("passwordNew") fieldName="password" autocomplete="new-password" autofocus=true />
            <@field.password name="password-confirm" label=msg("passwordConfirm") autocomplete="new-password" />

            <div class="tp-totp-logout">
                <@passwordCommons.logoutOtherSessions/>
            </div>

            <div class="tp-totp-actions">
                <#if isAppInitiatedAction??>
                    <button type="submit" name="cancel-aia" value="true"
                            class="${properties.kcButtonDefaultClass!}" id="kc-cancel">${msg("doCancel")}</button>
                    <button type="submit" name="login"
                            class="${properties.kcButtonPrimaryClass!}" id="kc-submit">${msg("updatePasswordSubmit")}</button>
                <#else>
                    <button type="submit" name="login"
                            class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" id="kc-submit">${msg("updatePasswordSubmit")}</button>
                </#if>
            </div>
        </form>

        <@validator.templates/>
        <@validator.script field="password-new"/>
    </#if>
</@layout.registrationLayout>
