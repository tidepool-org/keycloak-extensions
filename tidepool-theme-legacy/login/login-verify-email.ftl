<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
        <#-- Tidepool change: branded headline ("Keeping your data private and secure...") instead of upstream -->
        <#-- "Email verification". -->
        ${msg("keepingYourDataSecure")}
    <#elseif section = "form">
      <#-- Tidepool change: rework the instruction layout — a mail icon next to a single text block, instead of -->
      <#-- the upstream <p class="instruction"> plus a separate "info" section. The resend-link instruction -->
      <#-- (previously rendered from the "info" section) is now folded inline next to the main instruction so the -->
      <#-- whole message reads as one unit. -->
      <div class="instruction">
        <img src="${url.resourcesPath}/img/mail_icon.svg" alt="You've got mail" class="mail-icon">
        <div>
            <#if verifyEmail??>
                ${msg("emailVerifyInstruction1",verifyEmail)}
            <#else>
                ${msg("emailVerifyInstruction4",user.email)}
            </#if>
            <#if !isAppInitiatedAction??>
                ${msg("emailVerifyInstruction2")}
                <br/>
                <a href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailVerifyInstruction3")}
            </#if>
        </div>
      </div>
      <#if isAppInitiatedAction??>
          <form id="kc-verify-email-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
              <div class="${properties.kcFormGroupClass!}">
                  <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                      <#if verifyEmail??>
                          <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("emailVerifyResend")}" />
                      <#else>
                          <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("emailVerifySend")}" />
                      </#if>
                      <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" type="submit" name="cancel-aia" value="true" formnovalidate>${msg("doCancel")}</button>
                  </div>
              </div>
          </form>
      </#if>
    </#if>
</@layout.registrationLayout>
