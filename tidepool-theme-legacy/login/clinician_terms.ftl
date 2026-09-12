<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "form">
        <div id="kc-terms-text">
            <div class="terms-checkbox">
                <div id="terms-wrapper" class="clinician-terms-wrapper">
                    <label for="terms" onclick="updateClinicianTermsForm()">
                        <input type="checkbox" id="terms" name="terms" onclick="updateClinicianTermsForm()"> I accept the terms of the <a class="terms-link" href="https://tidepool.org/terms-of-use">Tidepool Applications Terms of Use</a> and <a class="terms-link" href="https://tidepool.org/privacy-policy">Privacy Policy</a>
                    </label>
                </div>
            </div>
            <form class="form-actions" action="${url.loginAction}" method="POST">
                <div class="center-form-buttons">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="cancel" id="kc-decline" type="submit" value="${msg("doDecline")}"/>
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="accept" id="kc-accept" type="submit" value="${msg("doAccept")}" disabled/>
                </div>
            </form>
            <div class="clearfix"></div>
        </div>
    </#if>
</@layout.registrationLayout>
