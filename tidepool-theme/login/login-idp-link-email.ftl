<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("emailLinkIdpTitle", idpDisplayName)}
    <#elseif section = "form">
            <div class="${properties.kcFormGroupClass!}">
                <p id="instruction1" class="instruction">
                    ${msg("emailLinkIdp1", idpDisplayName, brokerContext.username, realm.displayName)}
                </p>
                <div class="attempted-username instruction">
                    <input disabled
                           id="username"
                           class="${properties.kcInputClass!}"
                           type="text" autocomplete="off"
                           value="${brokerContext.username}"
                    />
                </div>
                <p id="instruction2" class="instruction">
                    <a class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!} tp-btn-fw"  href="${url.loginAction}">${msg("emailLinkIdpResendVerificationCode")}</a>
                </p>
                <p id="instruction3" class="instruction">
                    ${msg("emailLinkIdp4")} <a href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailLinkIdp5")}
                </p>
            </div>
    </#if>
</@layout.registrationLayout>