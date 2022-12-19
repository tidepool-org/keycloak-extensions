<#import "template.ftl" as layout>
<@layout.registrationLayout; section>
    <#if section = "header">
        ${msg("emailLinkIdpTitle", idpDisplayName)}
    <#elseif section = "form">
        <p id="instruction1" class="instruction">
            ${msg("emailLinkIdp1", idpDisplayName, brokerContext.username, realm.displayName)}
        </p>
        <p id="instruction2" class="instruction">
            <a href="${url.loginAction}">${msg("emailLinkIdpResendVerificationCode")}</a>
        </p>
        <p id="instruction3" class="instruction">
            ${msg("emailLinkIdp4")} <a href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailLinkIdp5")}
        </p>
    </#if>
</@layout.registrationLayout>