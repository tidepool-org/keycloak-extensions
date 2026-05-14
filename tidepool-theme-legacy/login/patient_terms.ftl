<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "form">
    <div id="kc-terms-text">
        <div class="terms-radio">
            <div>
                <label for="age>18" onclick="updatePatientTermsForm()">
                    <input type="radio" id="age>18" name="age" value=">18" onclick="updatePatientTermsForm()" checked> I am 18 years old or older.
                </label>
            </div>
            <div>
                <label for="age13-17" onclick="updatePatientTermsForm()">
                    <input type="radio" id="age13-17" name="age" value="13-17" onclick="updatePatientTermsForm()"> I am between 13 and 17 years old. You'll need to have a parent or guardian agree to the terms below.
                </label>
            </div>
            <div>
                <label for="age<13" onclick="updatePatientTermsForm()">
                    <input type="radio" id="age<13" name="age" value="<13" onclick="updatePatientTermsForm()"> I am 12 years old or younger.
                </label>
            </div>
        </div>
        <div class="terms-checkbox">
            <div id="terms-wrapper">
                <label for="terms" onclick="updatePatientTermsForm()">
                    <input type="checkbox" id="terms" name="terms" onclick="updatePatientTermsForm()"> I am 18 or older and I accept the terms of the <a class="terms-link" href="https://tidepool.org/terms-of-use">Tidepool Applications Terms of Use</a> and <a class="terms-link" href="https://tidepool.org/privacy-policy">Privacy Policy</a>
                </label>
            </div>
            <div id="terms-child-wrapper" style="display: none">
                <label for="terms-child" onclick="updatePatientTermsForm()">
                    <input type="checkbox" id="terms-child" name="terms-child" onclick="updatePatientTermsForm()"> I agree that my child aged 13 through 17 can use Tidepool Applications and agree that they are also bound to the terms of the <a class="terms-link" href="https://tidepool.org/terms-of-use">Tidepool Applications Terms of Use</a> and <a class="terms-link" href="https://tidepool.org/privacy-policy">Privacy Policy</a>
                </label>
            </div>
        </div>
        <div id="terms-sorry" style="display: none">
            We are really sorry, but you need to be 13 or older in order to create an account and use Tidepool's Applications.
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
