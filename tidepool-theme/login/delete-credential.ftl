<#import "template.ftl" as layout>
<#-- Tidepool override of keycloak.v2's delete-credential.ftl — the AIA prompt shown -->
<#-- when a user removes their OTP/2FA device. Figma file g8xYrHViRt9nd1oXx0OuIF, node -->
<#-- 11639:28634: "Delete <device>" title, a subtitle noting 2FA can be re-enabled -->
<#-- later, and a side-by-side Cancel / Delete pair (secondary + primary) reusing -->
<#-- the shared .tp-totp-actions grid. The form posts to url.loginAction; `accept` -->
<#-- confirms the deletion, `cancel-aia` aborts the application-initiated action. -->
<@layout.registrationLayout displayMessage=false subtitle=msg("deleteCredentialMessage", credentialLabel); section>
    <#if section = "header">
        ${msg("deleteCredentialTitle", credentialLabel)}
    <#elseif section = "form">
        <form id="kc-delete-credential-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="POST">
            <div class="tp-totp-actions">
                <button type="submit" name="cancel-aia" value="true"
                        class="${properties.kcButtonDefaultClass!}" id="kc-decline">${msg("doCancel")}</button>
                <button type="submit" name="accept"
                        class="${properties.kcButtonPrimaryClass!}" id="kc-accept">${msg("deleteCredentialConfirm")}</button>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
