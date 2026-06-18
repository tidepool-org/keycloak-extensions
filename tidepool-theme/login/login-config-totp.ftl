<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "password-commons.ftl" as passwordCommons>
<#-- Split TOTP setup into two visual steps with client-side step transitions: -->
<#--   1. QR-code step: shows the authenticator app list + QR code (or manual secret). -->
<#--   2. Verify step:  device label + 6-digit code + "log out other sessions". -->
<#-- Both steps share one POST form. JS-only navigation. If the server returns a -->
<#-- validation error on totp or userLabel, the form starts on step 2 so the error -->
<#-- is visible and re-submission picks up where the user left off. -->
<#assign hasFieldError = messagesPerField.existsError('totp','userLabel')>
<#assign initialStep = hasFieldError?then('verify','qr')>
<@layout.registrationLayout displayRequiredFields=false displayMessage=!hasFieldError; section>
    <#if section = "header">
        ${msg("loginTotpHeader")}
    <#elseif section = "form">
        <form action="${url.loginAction}"
              class="${properties.kcFormClass!} tp-totp-form"
              id="kc-totp-settings-form"
              method="post"
              novalidate="novalidate"
              data-step="${initialStep}">

            <input type="hidden" id="totpSecret" name="totpSecret" value="${totp.totpSecret}"/>
            <#if mode??><input type="hidden" id="mode" name="mode" value="${mode}"/></#if>

            <#-- ===================== Step 1: QR code ===================== -->
            <section class="tp-totp-step tp-totp-step--qr">
                <div class="tp-totp-apps">
                    <p class="tp-totp-section-title">${msg("loginTotpInstallApps")}</p>
                    <#-- Hardcoded list per the Figma design — the upstream -->
                    <#-- totp.supportedApplications list doesn't include EPIC/Duo -->
                    <#-- and uses different casing on the others. -->
                    <ul class="tp-totp-apps-list">
                        <li>${msg("loginTotpAppMicrosoft")}</li>
                        <li>${msg("loginTotpAppEpic")}</li>
                        <li>${msg("loginTotpAppDuo")}</li>
                        <li>${msg("loginTotpAppGoogle")}</li>
                    </ul>
                </div>

                <#if mode?? && mode = "manual">
                    <div class="tp-totp-secret">
                        <p class="tp-totp-section-title">${msg("loginTotpManualStep2")}</p>
                        <div class="tp-totp-secret-box">
                            <p class="tp-totp-secret-key" id="kc-totp-secret-key">${totp.totpSecretEncoded}</p>
                        </div>
                        <p class="tp-totp-mode-switch">
                            <a href="${totp.qrUrl}" id="mode-barcode">${msg("loginTotpScanBarcode")}</a>
                        </p>
                    </div>
                <#else>
                    <div class="tp-totp-qr-block">
                        <p class="tp-totp-section-title">${msg("loginTotpQrPrompt")}</p>
                        <div class="tp-totp-qr">
                            <img id="kc-totp-secret-qr-code" src="data:image/png;base64, ${totp.totpSecretQrCode}" alt="${msg("loginTotpQrAlt")}"/>
                        </div>
                        <p class="tp-totp-mode-switch">
                            <a href="${totp.manualUrl}" id="mode-manual">${msg("loginTotpUnableToScan")}</a>
                        </p>
                    </div>
                </#if>

                <div class="tp-totp-actions">
                    <#if isAppInitiatedAction??>
                        <button type="submit" name="cancel-aia" value="true"
                                class="${properties.kcButtonDefaultClass!}" id="cancelTOTPBtnStep1">${msg("doCancel")}</button>
                        <button type="button" class="${properties.kcButtonPrimaryClass!}" id="tp-totp-next">${msg("next")}</button>
                    <#else>
                        <button type="button" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" id="tp-totp-next">${msg("next")}</button>
                    </#if>
                </div>
            </section>

            <#-- ===================== Step 2: Verify code + name ===================== -->
            <section class="tp-totp-step tp-totp-step--verify">
                <div class="tp-totp-prompt">
                    <p class="tp-totp-section-title">${msg("loginTotpVerifyPrompt")}</p>
                    <p class="tp-totp-section-help">${msg("loginTotpVerifyHelp")}</p>
                </div>

                <@field.group name="userLabel" label=msg("loginTotpDeviceLabel") error=messagesPerField.get('userLabel') required=true>
                    <span class="${properties.kcInputClass!} <#if messagesPerField.existsError('userLabel')>${properties.kcError!}</#if>">
                        <input type="text" id="userLabel" name="userLabel" autocomplete="off" maxlength="200"
                               aria-invalid="<#if messagesPerField.existsError('userLabel')>true</#if>"/>
                        <@field.errorIcon error=messagesPerField.get('userLabel')/>
                    </span>
                </@field.group>

                <@field.group name="totp" label=msg("loginTotpCodeLabel") error=messagesPerField.get('totp') required=true>
                    <span class="${properties.kcInputClass!} <#if messagesPerField.existsError('totp')>${properties.kcError!}</#if>">
                        <input type="text" id="totp" name="totp" autocomplete="one-time-code" inputmode="numeric"
                               aria-invalid="<#if messagesPerField.existsError('totp')>true</#if>"/>
                        <@field.errorIcon error=messagesPerField.get('totp')/>
                    </span>
                </@field.group>

                <div class="tp-totp-logout">
                    <@passwordCommons.logoutOtherSessions/>
                </div>

                <div class="tp-totp-actions">
                    <#-- Always use a Back (JS) button on step 2 — it returns to step 1. -->
                    <#-- AIA users who want to abandon setup can use Cancel on step 1. -->
                    <button type="button" class="${properties.kcButtonDefaultClass!}" id="tp-totp-back">${msg("doBack")}</button>
                    <input type="submit"
                           class="${properties.kcButtonPrimaryClass!}"
                           id="saveTOTPBtn" value="${msg("loginTotpAddDevice")}"/>
                </div>
            </section>

            <p class="tp-totp-support">
                <a href="http://support.tidepool.org/" target="_blank" rel="noreferrer noopener">${msg("loginTotpSupportPrompt")}</a>
            </p>
        </form>

        <script>
            (function () {
                var form = document.getElementById('kc-totp-settings-form');
                if (!form) return;
                var nextBtn = document.getElementById('tp-totp-next');
                var backBtn = document.getElementById('tp-totp-back');
                var userLabel = document.getElementById('userLabel');
                var userLabelErrorContainer = document.getElementById('input-error-container-userLabel');
                var totp = document.getElementById('totp');
                var userLabelRequiredMessage = '${msg("loginTotpUserLabelRequired")?js_string}';

                function setStep(step) {
                    form.dataset.step = step;
                    if (step === 'verify') {
                        setTimeout(function () { userLabel && userLabel.focus(); }, 0);
                    }
                }

                if (nextBtn) {
                    nextBtn.addEventListener('click', function () { setStep('verify'); });
                }
                if (backBtn) {
                    backBtn.addEventListener('click', function () { setStep('qr'); clearUserLabelError(); });
                }

                function renderUserLabelError(message) {
                    if (!userLabelErrorContainer) return;
                    var helperText = document.createElement('div');
                    helperText.className = '${properties.kcFormHelperTextClass}';
                    helperText.setAttribute('aria-live', 'polite');
                    var inner = document.createElement('div');
                    inner.className = '${properties.kcInputHelperTextClass}';
                    var item = document.createElement('div');
                    item.className = '${properties.kcInputHelperTextItemClass} ${properties.kcError}';
                    item.id = 'input-error-userLabel';
                    var span = document.createElement('span');
                    span.className = '${properties.kcInputErrorMessageClass}';
                    span.textContent = message;
                    item.appendChild(span);
                    inner.appendChild(item);
                    helperText.appendChild(inner);
                    userLabelErrorContainer.innerHTML = '';
                    userLabelErrorContainer.appendChild(helperText);
                    userLabel.setAttribute('aria-invalid', 'true');
                    var wrapper = userLabel.parentElement;
                    if (wrapper) wrapper.classList.add('${properties.kcError}');
                }

                function clearUserLabelError() {
                    if (userLabelErrorContainer) userLabelErrorContainer.innerHTML = '';
                    if (!userLabel) return;
                    userLabel.removeAttribute('aria-invalid');
                    var wrapper = userLabel.parentElement;
                    if (wrapper) wrapper.classList.remove('${properties.kcError}');
                }

                form.addEventListener('submit', function (e) {
                    // Skip validation when the user is canceling — the cancel-aia
                    // submitter must reach the server even with empty fields.
                    var submitter = e.submitter || form.querySelector('button[type=submit]:focus');
                    if (submitter && submitter.name === 'cancel-aia') return;

                    if (userLabel && !userLabel.value.trim()) {
                        e.preventDefault();
                        setStep('verify');
                        renderUserLabelError(userLabelRequiredMessage);
                        userLabel.focus();
                    }
                });

                if (userLabel) {
                    userLabel.addEventListener('input', function () {
                        if (userLabel.value.trim()) clearUserLabelError();
                    });
                }
            })();
        </script>
    </#if>
</@layout.registrationLayout>
