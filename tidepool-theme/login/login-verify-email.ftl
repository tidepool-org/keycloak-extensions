<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
        ${msg("keepingYourDataSecure")}
    <#elseif section = "form">
      <div class="instruction">
        <img src="${url.resourcesPath}/img/mail_icon.svg" alt="You've got mail" class="mail-icon">
        <div>
            ${msg("emailVerifyInstruction1")}
            ${msg("emailVerifyInstruction2")}
          <br/>
          <a href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailVerifyInstruction3")}
        </div>
      </div>
    </#if>
</@layout.registrationLayout>