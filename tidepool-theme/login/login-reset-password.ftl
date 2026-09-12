<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayInfo=false displayMessage=!messagesPerField.existsError('username') subtitle=msg("emailResetLinkInstruction"); section>
    <#if section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <form id="kc-reset-password-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <#-- Tidepool change: this page is only reachable via the "Forgot -->
            <#-- password?" link on login-password.ftl, where an email has -->
            <#-- already been entered. Render the attempted username as the -->
            <#-- same read-only row used on the password step so the two -->
            <#-- screens feel like a single continuous flow. A hidden input -->
            <#-- carries the value to the server since the visible row is a -->
            <#-- div, not a form control. Falls back to an editable input -->
            <#-- only if the attempted username is somehow missing. -->
            <#if (auth.attemptedUsername!'')?has_content>
                <@layout.username/>
                <input type="hidden" name="username" value="${auth.attemptedUsername}"/>
            <#else>
                <#assign label>
                    <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
                </#assign>
                <@field.input name="username" label=label value=auth.attemptedUsername!'' autofocus=true />
            </#if>

            <#-- "Back to Login" is omitted: this page is only reached from the -->
            <#-- "Forgot password?" link after the user has already entered a valid -->
            <#-- email, so a back-to-login button would be misleading. No sign-up -->
            <#-- link either — the user already has an account by definition. -->
            <@buttons.actionGroup>
              <@buttons.button id="kc-form-buttons" label="next" class=["kcButtonPrimaryClass", "kcButtonBlockClass"]/>
            </@buttons.actionGroup>

        </form>
    <#elseif section = "info" >
        <span class="${properties.kcLoginMainFooterHelperText!}">
            <#if realm.duplicateEmailsAllowed>
                ${msg("emailInstructionUsername")}
            <#else>
                ${msg("emailInstruction")}
            </#if>
        </span>
    </#if>
</@layout.registrationLayout>
