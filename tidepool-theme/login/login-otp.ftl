<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "tp-commons.ftl" as tp>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp') subtitle=msg("loginOtpSubtitle"); section>
    <#if section = "header">
        ${msg("loginOtpTitle")}
    <#elseif section = "form">
        <#-- Resolve the device label for the "You will receive the code on" hint. -->
        <#-- Prefer the selected credential; fall back to the first if none is set. -->
        <#assign selectedLabel = "">
        <#if otpLogin?? && otpLogin.userOtpCredentials??>
            <#list otpLogin.userOtpCredentials as cred>
                <#if cred.id == (otpLogin.selectedCredentialId!'')>
                    <#assign selectedLabel = cred.userLabel!''>
                    <#break>
                </#if>
            </#list>
            <#if !selectedLabel?has_content && otpLogin.userOtpCredentials?size gt 0>
                <#assign selectedLabel = otpLogin.userOtpCredentials?first.userLabel!''>
            </#if>
        </#if>

        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <input id="selectedCredentialId" type="hidden" name="selectedCredentialId" value="${otpLogin.selectedCredentialId!''}">

            <#-- When there's no other authenticator to fall back on (Keycloak only -->
            <#-- offers "Try another way" when additional credentials exist), warn -->
            <#-- that the authenticator app is the user's only remaining option. -->
            <#if !(auth?has_content && auth.showTryAnotherWayLink())>
                <div class="tp-warning-banner" role="alert">
                    <span class="tp-warning-banner-icon" aria-hidden="true">
                        <svg viewBox="0 0 18 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M8.13 1.5a1 1 0 0 1 1.74 0l7.36 12.75A1 1 0 0 1 16.36 16H1.64a1 1 0 0 1-.87-1.75L8.13 1.5zM9 6a.75.75 0 0 0-.75.75v3.5a.75.75 0 0 0 1.5 0v-3.5A.75.75 0 0 0 9 6zm0 7a1 1 0 1 0 0-2 1 1 0 0 0 0 2z"/></svg>
                    </span>
                    <div class="tp-warning-banner-text">
                        <p class="tp-warning-banner-title">${msg("loginOtpNoAuthWarningTitle")}</p>
                        <p class="tp-warning-banner-body">${msg("loginOtpNoAuthWarningBody")}</p>
                    </div>
                </div>
            </#if>

            <#if otpLogin?? && otpLogin.userOtpCredentials?? && otpLogin.userOtpCredentials?size gt 1>
                <div class="tp-otp-device">
                    <span class="tp-otp-device-hint">${msg("loginOtpDeviceHint")}</span>
                    <div class="tp-otp-credentials" role="radiogroup" aria-label="${msg("loginChooseAuthenticator")}">
                        <#list otpLogin.userOtpCredentials as otpCredential>
                            <#assign isSelected = otpCredential.id == (otpLogin.selectedCredentialId!'')>
                            <label class="tp-otp-credential<#if isSelected> tp-otp-credential--selected</#if>">
                                <input type="radio" name="otpCredentialSelector" value="${otpCredential.id}"
                                       onchange="document.getElementById('selectedCredentialId').value=this.value;document.querySelectorAll('.tp-otp-credential').forEach(function(el){el.classList.remove('tp-otp-credential--selected');});this.parentElement.classList.add('tp-otp-credential--selected');"
                                       <#if isSelected>checked</#if>/>
                                <span class="tp-otp-credential-label">${otpCredential.userLabel}</span>
                            </label>
                        </#list>
                    </div>
                </div>
            <#elseif selectedLabel?has_content && selectedLabel?lower_case != "unnamed">
                <#-- Skip the device-name block when the credential has no real -->
                <#-- userLabel — Keycloak falls back to the literal string "unnamed" -->
                <#-- and showing "You will receive the code on / unnamed" looks broken. -->
                <div class="tp-otp-device">
                    <span class="tp-otp-device-hint">${msg("loginOtpDeviceHint")}</span>
                    <span class="tp-otp-device-name">${selectedLabel}</span>
                </div>
            </#if>

            <@field.input name="otp" label=msg("loginOtpFieldLabel") autocomplete="one-time-code" fieldName="totp" autofocus=true />

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcFormActionGroupClass!}">
                    <button class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" name="login" id="kc-login" type="submit">${msg("doLogIn")}</button>
                </div>
            </div>
        </form>

        <#-- Custom "Try another way" link rendered as an inline sentence to match -->
        <#-- the Figma design. Submits the standard tryAnotherWay=on POST so the -->
        <#-- flow continues with selectAuthenticator. The template-level form is -->
        <#-- hidden via CSS (#kc-select-try-another-way-form) so it doesn't double. -->
        <#if auth?has_content && auth.showTryAnotherWayLink()>
            <form id="tp-otp-try-another-way" action="${url.loginAction}" method="post">
                <input type="hidden" name="tryAnotherWay" value="on"/>
                <p class="tp-otp-try-another-way">
                    ${msg("loginOtpTryAnotherWayPrefix")}
                    <a href="javascript:document.forms['tp-otp-try-another-way'].requestSubmit()">${msg("loginOtpTryAnotherWay")}</a>
                </p>
            </form>
        </#if>

        <@tp.otpDisclaimer/>
    </#if>
</@layout.registrationLayout>
