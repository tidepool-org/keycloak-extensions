<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "user-profile-commons.ftl" as userProfileCommons>
<@layout.registrationLayout displayMessage=messagesPerField.exists('global') displayRequiredFields=false displayInfo=true; section>
    <#if section = "header">
        ${msg("registerTitlePersonal")}
    <#elseif section = "form">
        <form id="kc-register-form" class="${properties.kcFormClass!}" action="${url.registrationAction}" method="post">

            <@userProfileCommons.userProfileFormFields; callback, attribute>
                <#if callback = "afterField">
                    <#-- Insert password fields right after the email/username (the field that's used as the login identifier). -->
                    <#if passwordRequired?? && (attribute.name == 'username' || (attribute.name == 'email' && realm.registrationEmailAsUsername))>
                        <@field.password name="password" label=msg("password") autocomplete="new-password" />
                        <@field.password name="password-confirm" label=msg("passwordConfirm") autocomplete="new-password" />
                    </#if>
                </#if>
            </@userProfileCommons.userProfileFormFields>

            <#if recaptchaRequired?? && (recaptchaVisible!false)>
                <div class="${properties.kcFormGroupClass!}">
                    <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}" data-action="${recaptchaAction}"></div>
                </div>
            </#if>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcFormActionGroupClass!}">
                    <#if recaptchaRequired?? && !(recaptchaVisible!false)>
                        <script>function onSubmitRecaptcha(token) { document.getElementById("kc-register-form").submit(); }</script>
                        <button class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} g-recaptcha"
                                data-sitekey="${recaptchaSiteKey}" data-callback="onSubmitRecaptcha" data-action="${recaptchaAction}" type="submit">
                            ${msg("doCreateAccount")}
                        </button>
                    <#else>
                        <button class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" name="register" id="kc-register" type="submit">${msg("doCreateAccount")}</button>
                    </#if>
                </div>
            </div>
        </form>
    <#elseif section = "info">
        <div id="kc-registration">
            <span>${msg("alreadyHaveAnAccount")} <a href="${url.loginRestartFlowUrl}">${msg("doLogIn")}</a></span>
        </div>
        <#if role?? && role.registrationUriForClinicianRole??>
            <div id="kc-registration-clinician">
                <span>${msg("needClinicianAccount")} ${msg("createAccountPrefix")} <a href="${role.registrationUriForClinicianRole}">${msg("createAccountClinician")}</a> ${msg("createAccountSuffix")}</span>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
