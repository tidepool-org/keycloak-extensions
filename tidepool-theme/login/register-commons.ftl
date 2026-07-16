<#-- Tidepool rewrite of the upstream termsAcceptance macro. Upstream emits two -->
<#-- stacked form-groups (a "Terms" body block + an "I agree" checkbox named -->
<#-- termsAccepted); Tidepool collapses both into a single inline checkbox with -->
<#-- ToU + Privacy Policy links built into the label. The field name is "terms" -->
<#-- (not "termsAccepted") to match Tidepool's server-side terms-validation -->
<#-- logic and clinician_terms.ftl / patient_terms.ftl. -->
<#-- This rev wraps the input + label with the pf-v5-c-check classes so it -->
<#-- picks up the same custom-skinned checkbox styling as the "Remember me" -->
<#-- field on login-username.ftl. -->
<#macro termsAcceptance>
    <#if termsAcceptanceRequired??>
        <div class="${properties.kcFormGroupClass!}">
            <div class="pf-v5-c-check">
                <input type="checkbox" id="terms" name="terms" class="pf-v5-c-check__input"
                       aria-invalid="<#if messagesPerField.existsError('terms')>true</#if>"/>
                <label for="terms" class="pf-v5-c-check__label">
                    I accept the terms of the
                    <a class="tp-inline-link" href="https://tidepool.org/terms-of-use" target="_blank" rel="noreferrer noopener">Tidepool Applications Terms of Use</a>
                    and
                    <a class="tp-inline-link" href="https://tidepool.org/privacy-policy" target="_blank" rel="noreferrer noopener">Privacy Policy</a>
                </label>
            </div>
            <#if messagesPerField.existsError('terms')>
                <div id="input-error-container-terms">
                    <div class="${properties.kcFormHelperTextClass!}" aria-live="polite">
                        <div class="${properties.kcInputHelperTextClass!}">
                            <div class="${properties.kcInputHelperTextItemClass!} ${properties.kcError!}">
                                <span class="${properties.kcInputErrorMessageClass!}">${kcSanitize(messagesPerField.get('terms'))?no_esc}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </#if>
        </div>
    </#if>
</#macro>

<#-- Disables the Sign Up button until every required field is filled in (and -->
<#-- the terms checkbox, when present, is checked). Required fields are the -->
<#-- ones whose form-group label carries the required-asterisk span — field.ftl -->
<#-- never emits the HTML required attribute, so the marker is the only signal. -->
<#-- The button is enabled in markup and disabled from JS so the form still -->
<#-- submits without JS; server-side validation remains the authority. -->
<#macro completionGate>
    <script>
        (function () {
            var form = document.getElementById('kc-register-form');
            if (!form) return;
            var submit = form.querySelector('button[type="submit"]');
            if (!submit) return;

            function isComplete() {
                var fields = form.querySelectorAll('input, select, textarea');
                for (var i = 0; i < fields.length; i++) {
                    var f = fields[i];
                    if (f.type === 'hidden' || f.type === 'submit' || f.type === 'button' || f.disabled) continue;
                    if (!f.getClientRects().length) continue;
                    if (f.type === 'checkbox') {
                        if (f.id === 'terms' && !f.checked) return false;
                        continue;
                    }
                    var group = f.closest('.pf-v5-c-form__group');
                    if (group && group.querySelector('.pf-v5-c-form__label-required') && !f.value.trim()) {
                        return false;
                    }
                }
                return true;
            }

            function update() { submit.disabled = !isComplete(); }

            form.addEventListener('input', update);
            form.addEventListener('change', update);
            update();
        })();
    </script>
</#macro>
