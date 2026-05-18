<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayInfo=false subtitle=msg("selectAuthSubtitle"); section>
    <#if section = "header">
        ${msg("selectAuthTitle")}
    <#elseif section = "form">
        <form id="kc-select-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <input type="hidden" name="authenticationExecution" id="authenticationExecution" value=""/>

            <div class="tp-auth-options" role="radiogroup" aria-label="${msg("selectAuthTitle")}">
                <#list auth.authenticationSelections as authenticationSelection>
                    <label class="tp-auth-option">
                        <input type="radio" name="authSelection"
                               value="${authenticationSelection.authExecId}"
                               class="tp-auth-option-input"
                               data-icon="${authenticationSelection.iconCssClass}"/>
                        <span class="tp-auth-option-card">
                            <span class="tp-auth-option-icon" aria-hidden="true">
                                <#if authenticationSelection.iconCssClass?contains("Recovery")>
                                    <#-- Key icon for Recovery Codes (figma fi-bs-key). -->
                                    <svg viewBox="0 0 20 20" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M13 1a6 6 0 0 0-5.66 8.01L1 15.34V19h3.66l1-1v-2h2v-2h2l1.32-1.34A6 6 0 1 0 13 1zm1 6.5a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z"/></svg>
                                <#else>
                                    <#-- Lock icon for 2FA / OTP / default (figma fi-bs-lock). -->
                                    <svg viewBox="0 0 20 20" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M15 8V6a5 5 0 0 0-10 0v2H4a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2h-1zM7 6a3 3 0 1 1 6 0v2H7V6zm4 8.73V16a1 1 0 0 1-2 0v-1.27a2 2 0 1 1 2 0z"/></svg>
                                </#if>
                            </span>
                            <span class="tp-auth-option-text">
                                <span class="tp-auth-option-title">${msg('${authenticationSelection.displayName}')}</span>
                                <span class="tp-auth-option-description">${msg('${authenticationSelection.helpText}')}</span>
                            </span>
                            <span class="tp-auth-option-check" aria-hidden="true">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                            </span>
                        </span>
                    </label>
                </#list>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcFormActionGroupClass!}">
                    <button id="kc-next" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" type="submit" disabled>${msg("next")}</button>
                </div>
            </div>
        </form>

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

        <script>
            document.querySelectorAll('input[name="authSelection"]').forEach(function (r) {
                r.addEventListener('change', function () {
                    document.getElementById('authenticationExecution').value = this.value;
                    document.getElementById('kc-next').disabled = false;
                });
            });
        </script>
    </#if>
</@layout.registrationLayout>
