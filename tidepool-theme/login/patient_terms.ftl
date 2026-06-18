<#import "template.ftl" as layout>
<#-- Tidepool redesign of the personal-account terms screen to match the 2FA -->
<#-- Figma (file g8xYrHViRt9nd1oXx0OuIF, node 11691:48930): a "Confirm details -->
<#-- to continue" title, an age radio group (gray copy, custom indigo radios), -->
<#-- the terms-acceptance checkbox(es), and a single full-width "Next" button. -->
<#-- The radios/checkboxes live outside the posting form; updatePatientTermsForm() -->
<#-- (resources/js/terms.js) reads them to toggle visibility and enable the -->
<#-- accept button. The form posts only `accept` to url.loginAction. -->
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("patientTermsTitle")}
    <#elseif section = "form">
    <div id="kc-terms-text" class="tp-terms">
        <fieldset class="tp-terms-age">
            <label class="tp-terms-age-option">
                <input type="radio" id="age>18" name="age" value=">18" class="tp-terms-age-input" onclick="updatePatientTermsForm()" checked>
                <span class="tp-terms-age-mark" aria-hidden="true"></span>
                <span class="tp-terms-age-label">I am 18 years old or older.</span>
            </label>
            <label class="tp-terms-age-option">
                <input type="radio" id="age13-17" name="age" value="13-17" class="tp-terms-age-input" onclick="updatePatientTermsForm()">
                <span class="tp-terms-age-mark" aria-hidden="true"></span>
                <span class="tp-terms-age-label">I am between 13 and 17 years old. You'll need to have a parent or guardian agree to the terms below.</span>
            </label>
            <label class="tp-terms-age-option">
                <input type="radio" id="age<13" name="age" value="<13" class="tp-terms-age-input" onclick="updatePatientTermsForm()">
                <span class="tp-terms-age-mark" aria-hidden="true"></span>
                <span class="tp-terms-age-label">I am 12 years old or younger.</span>
            </label>
        </fieldset>

        <div class="tp-terms-accept">
            <div id="terms-wrapper" class="pf-v5-c-check">
                <input type="checkbox" id="terms" name="terms" class="pf-v5-c-check__input" onclick="updatePatientTermsForm()">
                <label for="terms" class="pf-v5-c-check__label">I am 18 or older and I accept the terms of the <a class="tp-inline-link" href="https://tidepool.org/terms-of-use">Tidepool Applications Terms of Use</a> and <a class="tp-inline-link" href="https://tidepool.org/privacy-policy">Privacy Policy</a></label>
            </div>
            <div id="terms-child-wrapper" class="pf-v5-c-check" style="display: none">
                <input type="checkbox" id="terms-child" name="terms-child" class="pf-v5-c-check__input" onclick="updatePatientTermsForm()">
                <label for="terms-child" class="pf-v5-c-check__label">I agree that my child aged 13 through 17 can use Tidepool Applications and agree that they are also bound to the terms of the <a class="tp-inline-link" href="https://tidepool.org/terms-of-use">Tidepool Applications Terms of Use</a> and <a class="tp-inline-link" href="https://tidepool.org/privacy-policy">Privacy Policy</a></label>
            </div>
            <div id="terms-sorry" class="tp-terms-sorry" style="display: none">
                We are really sorry, but you need to be 13 or older in order to create an account and use Tidepool's Applications.
            </div>
            <form class="tp-terms-form" action="${url.loginAction}" method="POST">
                <button class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" name="accept" id="kc-accept" type="submit" disabled>${msg("next")}</button>
            </form>
        </div>
    </div>
    </#if>
</@layout.registrationLayout>
