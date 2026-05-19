<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "social-providers.ftl" as identityProviders>
<#import "passkeys.ftl" as passkeys>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <form id="kc-form-login" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                        <#if !usernameHidden??>
                            <#assign usernameLabel>
                                <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
                            </#assign>
                            <#assign usernameError = messagesPerField.get('username')>
                            <@field.group name="username" label=usernameLabel error=usernameError>
                                <span class="${properties.kcInputClass!} <#if usernameError?has_content>${properties.kcError!}</#if>">
                                    <input id="username"
                                           name="username"
                                           value="${(login.username!'')}"
                                           type="text"
                                           autofocus
                                           autocomplete="${(enableWebAuthnConditionalUI?has_content)?then('username webauthn', 'username')}"
                                           placeholder="${msg('enterEmail')}"
                                           aria-invalid="<#if usernameError?has_content>true</#if>" />
                                    <@field.errorIcon error=usernameError />
                                </span>
                            </@field.group>
                        <#else>
                            <#-- AIA re-auth path: there's no editable username input, but -->
                            <#-- we still know who's signing in (auth.attemptedUsername). -->
                            <#-- Render the read-only attempted-username row so the user -->
                            <#-- can confirm the identity (and "Not you?" to restart). -->
                            <@layout.username />
                        </#if>

                        <#if realm.rememberMe && !usernameHidden??>
                            <div class="${properties.kcFormGroupClass!}">
                                <@field.checkbox name="rememberMe" label=msg("rememberMe") value=login.rememberMe?? />
                            </div>
                        </#if>

                        <div class="${properties.kcFormGroupClass!}">
                            <div class="${properties.kcFormActionGroupClass!}">
                                <button class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" name="login" id="kc-login" type="submit">${msg("next")}</button>
                            </div>
                        </div>
                    </form>
                </#if>
            </div>
        </div>
        <@passkeys.conditionalUIData />

    <#elseif section = "info">
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    <#elseif section = "socialProviders">
        <#if realm.password && social.providers?? && social.providers?has_content>
            <@identityProviders.show social=social />
        </#if>
    </#if>
</@layout.registrationLayout>
