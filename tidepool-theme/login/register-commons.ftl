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
