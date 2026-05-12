<#import "template.ftl" as layout>
<#import "passkeys.ftl" as passkeys>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        <#-- Tidepool change: branded headline replacing upstream "Sign in to your account". -->
        ${msg("letsGetStarted")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}"
                          method="post">
                        <#if !usernameHidden??>
                            <div class="${properties.kcFormGroupClass!}">
                                <#-- Tidepool change: label uses prompt-style strings ("Enter your email" etc.) instead of -->
                                <#-- the upstream bare noun ("Email"). The bare noun is reused below as the input placeholder, -->
                                <#-- so the field reads as "Enter your email" / placeholder "Email". -->
                                <label for="username"
                                       class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("enterYourUsername")}<#elseif !realm.registrationEmailAsUsername>${msg("enterYourUsernameOrEmail")}<#else>${msg("enterYourEmail")}</#if></label>

                                <input tabindex="1" id="username"
                                       aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                                       class="${properties.kcInputClass!}" name="username"
                                       value="${(login.username!'')}"
                                       type="text" autofocus
                                       autocomplete="${(enableWebAuthnConditionalUI?has_content)?then('username webauthn', 'username')}"
                                       placeholder="<#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>"
                                       dir="ltr"/>

                                <#-- Tidepool change: <span> -> <div> so the error message renders as a block under the input -->
                                <#-- (the tp-form-error class assumes block-level layout). -->
                                <#if messagesPerField.existsError('username')>
                                    <div id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                        ${kcSanitize(messagesPerField.get('username'))?no_esc}
                                    </div>
                                </#if>
                            </div>
                        </#if>

                        <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                            <div id="kc-form-options">
                                <#if realm.rememberMe && !usernameHidden??>
                                    <div class="checkbox">
                                        <label>
                                            <#if login.rememberMe??>
                                                <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"
                                                       checked> ${msg("rememberMe")}
                                            <#else>
                                                <input tabindex="3" id="rememberMe" name="rememberMe"
                                                       type="checkbox"> ${msg("rememberMe")}
                                            </#if>
                                        </label>
                                    </div>
                                </#if>
                            </div>
                        </div>

                        <#-- Tidepool change: button label is "Next" (this is step 1 of a two-step flow that proceeds to -->
                        <#-- the password screen in login.ftl), not the upstream "Sign In". -->
                        <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                            <input tabindex="4"
                                   class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                   name="login" id="kc-login" type="submit" value="${msg("next")}"/>
                        </div>
                    </form>
                </#if>
            </div>
        </div>
        <@passkeys.conditionalUIData />

    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration">
                <span>${msg("noAccount")} <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a></span>
            </div>
        </#if>
    <#elseif section = "socialProviders" >
        <#if realm.password && social?? && social.providers?has_content>
            <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                <hr/>
                <h4>${msg("identity-provider-login-label")}</h4>

                <ul class="${properties.kcFormSocialAccountListClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountListGridClass!}</#if>">
                    <#list social.providers as p>
                        <a data-once-link data-disabled-class="${properties.kcFormSocialAccountListButtonDisabledClass!}" id="social-${p.alias}"
                                class="${properties.kcFormSocialAccountListButtonClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
                                type="button" href="${p.loginUrl}">
                            <#if p.iconClasses?has_content>
                                <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
                                <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                            <#else>
                                <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                            </#if>
                        </a>
                    </#list>
                </ul>
            </div>
        </#if>
    <#-- Tidepool addition: render the Tidepool footer (consumed by the "footer" slot in template.ftl). -->
    <#elseif section = "footer" >
        <#include "./partials/footer.ftl">
    </#if>

</@layout.registrationLayout>
