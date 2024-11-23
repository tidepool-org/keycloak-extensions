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
