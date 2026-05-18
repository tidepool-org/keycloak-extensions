<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false displayRequiredFields=false; section>
    <#if section = "header">
        ${msg("recovery-code-config-header")}
    <#elseif section = "form">
        <div class="tp-recovery-intro">
            <p class="tp-recovery-intro-title">${msg("loginRecoveryConfigDownloadTitle")}</p>
            <p class="tp-recovery-intro-body">${msg("loginRecoveryConfigDownloadDescription")}</p>
            <p class="tp-recovery-learn-more">
                <a href="http://support.tidepool.org/" target="_blank" rel="noreferrer noopener">${msg("loginRecoveryConfigLearnMore")}</a>
            </p>
        </div>

        <div class="tp-recovery-codes-block">
            <div class="tp-recovery-warning" role="alert">
                <span class="tp-recovery-warning-icon" aria-hidden="true">
                    <svg viewBox="0 0 18 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M8.13 1.5a1 1 0 0 1 1.74 0l7.36 12.75A1 1 0 0 1 16.36 16H1.64a1 1 0 0 1-.87-1.75L8.13 1.5zM9 6a.75.75 0 0 0-.75.75v3.5a.75.75 0 0 0 1.5 0v-3.5A.75.75 0 0 0 9 6zm0 7a1 1 0 1 0 0-2 1 1 0 0 0 0 2z"/></svg>
                </span>
                <div class="tp-recovery-warning-text">
                    <p class="tp-recovery-warning-title">${msg("recovery-code-config-warning-title")}</p>
                    <p class="tp-recovery-warning-body">${msg("recovery-code-config-warning-message")}</p>
                </div>
            </div>

            <div class="tp-recovery-codes">
                <ol id="kc-recovery-codes-list" class="tp-recovery-codes-list">
                    <#list recoveryAuthnCodesConfigBean.generatedRecoveryAuthnCodesList as code>
                        <li>${code[0..3]}-${code[4..7]}-${code[8..]}</li>
                    </#list>
                </ol>
            </div>

            <div class="tp-recovery-actions">
                <button id="printRecoveryCodes" class="tp-recovery-action" type="button" onclick="printRecoveryCodes()">
                    <svg viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M5 1a1 1 0 0 0-1 1v2h8V2a1 1 0 0 0-1-1H5zm-1 7v6a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V8H4zm2 1.5h4a.5.5 0 0 1 0 1H6a.5.5 0 0 1 0-1zm0 2h4a.5.5 0 0 1 0 1H6a.5.5 0 0 1 0-1zM2 5a2 2 0 0 0-2 2v4a2 2 0 0 0 2 2h1V8a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v5h1a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2H2zm10.5 2a.75.75 0 1 1 1.5 0 .75.75 0 0 1-1.5 0z"/></svg>
                    ${msg("recovery-codes-print")}
                </button>
                <button id="downloadRecoveryCodes" class="tp-recovery-action" type="button" onclick="downloadRecoveryCodes()">
                    <svg viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M8 1a.75.75 0 0 1 .75.75v6.69l2.22-2.22a.75.75 0 0 1 1.06 1.06l-3.5 3.5a.75.75 0 0 1-1.06 0l-3.5-3.5a.75.75 0 0 1 1.06-1.06l2.22 2.22V1.75A.75.75 0 0 1 8 1zM2.75 12a.75.75 0 0 1 .75.75V14h9v-1.25a.75.75 0 0 1 1.5 0V14a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 2 14v-1.25a.75.75 0 0 1 .75-.75z"/></svg>
                    ${msg("recovery-codes-download")}
                </button>
                <button id="copyRecoveryCodes" class="tp-recovery-action" type="button" onclick="copyRecoveryCodes()">
                    <svg viewBox="0 0 16 16" fill="currentColor" xmlns="http://www.w3.org/2000/svg"><path d="M5 2a2 2 0 0 1 2-2h5a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V2zm2-.5a.5.5 0 0 0-.5.5v8a.5.5 0 0 0 .5.5h5a.5.5 0 0 0 .5-.5V2a.5.5 0 0 0-.5-.5H7z"/><path d="M3 4a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h5a2 2 0 0 0 2-2v-1H8.5v1a.5.5 0 0 1-.5.5H3a.5.5 0 0 1-.5-.5V6a.5.5 0 0 1 .5-.5h1V4H3z"/></svg>
                    <span data-default-label="${msg("recovery-codes-copy")}" data-copied-label="${msg("recovery-codes-copied")}">${msg("recovery-codes-copy")}</span>
                </button>
            </div>
        </div>

        <form action="${url.loginAction}" class="${properties.kcFormClass!} tp-recovery-form" id="kc-recovery-codes-settings-form" method="post">
            <input type="hidden" name="generatedRecoveryAuthnCodes" value="${recoveryAuthnCodesConfigBean.generatedRecoveryAuthnCodesAsString}"/>
            <input type="hidden" name="generatedAt" value="${recoveryAuthnCodesConfigBean.generatedAt?c}"/>
            <input type="hidden" id="userLabel" name="userLabel" value="${msg("recovery-codes-label-default")}"/>

            <div class="tp-recovery-confirm">
                <label class="tp-recovery-confirm-label">
                    <input type="checkbox" id="kcRecoveryCodesConfirmationCheck" name="kcRecoveryCodesConfirmationCheck"
                           onchange="document.getElementById('saveRecoveryAuthnCodesBtn').disabled = !this.checked;"/>
                    <span>${msg("recovery-codes-confirmation-message")}</span>
                </label>
            </div>

            <div class="tp-totp-actions">
                <#if isAppInitiatedAction??>
                    <button type="submit" name="cancel-aia" value="true"
                            class="${properties.kcButtonDefaultClass!}" id="cancelRecoveryAuthnCodesBtn">${msg("recovery-codes-action-cancel")}</button>
                    <input type="submit"
                           class="${properties.kcButtonPrimaryClass!}"
                           id="saveRecoveryAuthnCodesBtn"
                           value="${msg("recovery-codes-action-complete")}"
                           disabled/>
                <#else>
                    <input type="submit"
                           class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}"
                           id="saveRecoveryAuthnCodesBtn"
                           value="${msg("recovery-codes-action-complete")}"
                           disabled/>
                </#if>
            </div>
        </form>

        <p class="tp-totp-support">
            <a href="http://support.tidepool.org/" target="_blank" rel="noreferrer noopener">${msg("loginTotpSupportPrompt")}</a>
        </p>

        <script>
            function parseRecoveryCodeList() {
                const items = document.getElementById("kc-recovery-codes-list").getElementsByTagName("li");
                let out = "";
                for (let i = 0; i < items.length; i++) {
                    <#noparse>
                    out += `${i+1}: ${items[i].innerText}\r\n`;
                    </#noparse>
                }
                return out;
            }

            function copyRecoveryCodes() {
                const text = parseRecoveryCodeList();
                const ok = () => {
                    const btn = document.getElementById('copyRecoveryCodes');
                    const span = btn.querySelector('span');
                    if (!span) return;
                    span.textContent = span.dataset.copiedLabel;
                    setTimeout(() => { span.textContent = span.dataset.defaultLabel; }, 1500);
                };
                if (navigator.clipboard && navigator.clipboard.writeText) {
                    navigator.clipboard.writeText(text).then(ok, () => fallbackCopy(text, ok));
                } else {
                    fallbackCopy(text, ok);
                }
            }
            function fallbackCopy(text, onDone) {
                const ta = document.createElement('textarea');
                ta.value = text;
                ta.style.position = 'fixed';
                ta.style.opacity = '0';
                document.body.appendChild(ta);
                ta.select();
                try { document.execCommand('copy'); onDone && onDone(); } catch (e) {}
                document.body.removeChild(ta);
            }

            function formatCurrentDateTime() {
                return new Date().toLocaleString('en-US', {
                    month: 'long', day: 'numeric', year: 'numeric',
                    hour: 'numeric', minute: 'numeric', timeZoneName: 'short'
                });
            }

            function buildDownloadContent() {
                return "${msg("recovery-codes-download-file-header")?js_string}\n\n" +
                    parseRecoveryCodeList() + "\n" +
                    "${msg("recovery-codes-download-file-description")?js_string}\n\n" +
                    "${msg("recovery-codes-download-file-date")?js_string} " + formatCurrentDateTime();
            }

            function downloadRecoveryCodes() {
                const el = document.createElement('a');
                el.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(buildDownloadContent()));
                el.setAttribute('download', 'tidepool-recovery-codes.txt');
                el.style.display = 'none';
                document.body.appendChild(el);
                el.click();
                document.body.removeChild(el);
            }

            function printRecoveryCodes() {
                const listHTML = document.getElementById('kc-recovery-codes-list').outerHTML;
                const styles =
                    '@page { size: auto; margin-top: 0; }' +
                    'body { width: 480px; font-family: sans-serif; }' +
                    'ol { font-family: monospace; padding-left: 24px; }' +
                    'p:first-of-type { margin-top: 48px; }';
                const html =
                    '<html><head><title>tidepool-recovery-codes</title><style>' + styles + '</style></head><body>' +
                    '<p>' + "${msg("recovery-codes-download-file-header")?js_string}" + '</p>' +
                    listHTML +
                    '<p>' + "${msg("recovery-codes-download-file-description")?js_string}" + '</p>' +
                    '<p>' + "${msg("recovery-codes-download-file-date")?js_string} " + formatCurrentDateTime() + '</p>' +
                    '</body></html>';
                const w = window.open();
                w.document.write(html);
                w.document.close();
                w.print();
                w.close();
            }
        </script>
    </#if>
</@layout.registrationLayout>
