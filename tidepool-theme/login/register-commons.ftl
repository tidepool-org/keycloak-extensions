<#-- Tidepool change: complete rewrite of the upstream termsAcceptance macro. -->
<#-- Upstream renders two stacked form-groups: a "Terms and Conditions" body block (msg("termsText")) plus a -->
<#-- separate "I agree" checkbox named "termsAccepted". Tidepool collapses this into a single inline checkbox -->
<#-- with the Tidepool ToU and Privacy Policy links built directly into the label (hard-coded URLs, not msg -->
<#-- keys, because both links are Tidepool-global and never localized/per-realm). -->
<#-- Note the form field is renamed from "termsAccepted" to "terms" to match the field name expected by -->
<#-- Tidepool's terms-validation logic on the server side (and by clinician_terms.ftl / patient_terms.ftl). -->
<#macro termsAcceptance>
    <#if termsAcceptanceRequired??>
        <div class="${properties.kcFormGroupClass!}">
            <div id="kc-terms-text">
                <div class="terms-checkbox">
                    <div id="terms-wrapper" class="clinician-terms-wrapper">
                        <input type="checkbox" id="terms" name="terms"/>
                        <label for="terms">I accept the terms of the <a class="terms-link" href="https://tidepool.org/terms-of-use">Tidepool Applications Terms of Use</a> and <a class="terms-link" href="https://tidepool.org/privacy-policy">Privacy Policy</a></label>
                    </div>
                    <#if messagesPerField.existsError('terms')>
                        <span id="input-error-terms" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                ${kcSanitize(messagesPerField.get('terms'))?no_esc}
                            </span>
                    </#if>
                </div>
            </div>
        </div>
    </#if>
</#macro>
