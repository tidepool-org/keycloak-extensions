<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "password-commons.ftl" as passwordCommons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('email') displayRequiredFields=false subtitle=msg("updateEmailSubtitle"); section>
    <#if section = "header">
        ${msg("updateEmailTitle")}
    <#elseif section = "form">
        <#-- The upstream template renders the email field via -->
        <#-- userProfileCommons.userProfileFormFields. For Tidepool we only -->
        <#-- ever ask for an email here (no other profile attributes), so -->
        <#-- render a single field directly to control the label and -->
        <#-- bypass the "Sign out from other devices" checkbox that upstream -->
        <#-- adds — neither is in the Figma. -->
        <#-- profile.attributes is a sequence of attribute objects, not a hash. -->
        <#-- Walk it to find the current email value so a failed submission -->
        <#-- doesn't blank the field out. -->
        <#assign emailValue = ''>
        <#if profile?? && profile.attributes??>
            <#list profile.attributes as attr>
                <#if attr.name == 'email'>
                    <#assign emailValue = (attr.value)!''>
                    <#break>
                </#if>
            </#list>
        </#if>
        <#assign emailError = messagesPerField.get('email')>
        <form id="kc-update-email-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <@field.input name="email" label=msg("updateEmailFieldLabel") value=emailValue error=emailError fieldName="email" autofocus=true autocomplete="email" />

            <div class="tp-totp-logout">
                <@passwordCommons.logoutOtherSessions/>
            </div>

            <div class="tp-totp-actions">
                <#if isAppInitiatedAction??>
                    <button type="submit" name="cancel-aia" value="true"
                            class="${properties.kcButtonDefaultClass!}" id="kc-cancel">${msg("doCancel")}</button>
                    <input type="submit" class="${properties.kcButtonPrimaryClass!}"
                           id="kc-submit" value="${msg("updateEmailSubmit")}"/>
                <#elseif updateEmailCancelAllowed??>
                    <#-- CancelableUpdateEmail sets this when the change can be abandoned (enforced AIA, or an -->
                    <#-- unverified pending email change exists). The button clears the pending change and -->
                    <#-- keeps the current email; formnovalidate so the empty email field doesn't block submit. -->
                    <button type="submit" name="cancel-update-email" value="true" formnovalidate
                            class="${properties.kcButtonDefaultClass!}" id="kc-cancel">${msg("doCancel")}</button>
                    <input type="submit" class="${properties.kcButtonPrimaryClass!}"
                           id="kc-submit" value="${msg("updateEmailSubmit")}"/>
                <#else>
                    <input type="submit"
                           class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}"
                           id="kc-submit" value="${msg("updateEmailSubmit")}"/>
                </#if>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
