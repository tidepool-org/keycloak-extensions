<#import "template.ftl" as layout>
<#-- Tidepool override of keycloak.v2's login-idp-link-confirm.ftl — the -->
<#-- first-broker-login confirmation shown when an account already exists and the -->
<#-- user is about to link an identity provider to it. Figma file -->
<#-- g8xYrHViRt9nd1oXx0OuIF, node 13831:31031: an IDP-named title, two body -->
<#-- paragraphs explaining the link (and a "stop if you didn't request this" -->
<#-- warning), and a single full-width "Next" button. The form posts -->
<#-- submitAction=linkAccount to url.loginAction to perform the link. -->
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("confirmLinkIdpTitle", idpDisplayName)}
    <#elseif section = "form">
        <div class="tp-idp-link-confirm">
            <div class="tp-idp-link-confirm-body">
                <p>${msg("confirmLinkIdpBody1", idpDisplayName)}</p>
                <p>${msg("confirmLinkIdpBody2")}</p>
            </div>
            <form id="kc-register-form" action="${url.loginAction}" method="post">
                <button type="submit" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}"
                        name="submitAction" id="linkAccount" value="linkAccount">${msg("next")}</button>
            </form>
        </div>
    </#if>
</@layout.registrationLayout>
