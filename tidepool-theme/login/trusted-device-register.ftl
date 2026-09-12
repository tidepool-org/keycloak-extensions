<#import "template.ftl" as layout>
<#-- Tidepool override of the SPI-bundled trusted-device-register.ftl. -->
<#-- The SPI ships a basic two-button form with an explanation paragraph; -->
<#-- we restyle it to match the Figma redesign (file g8xYrHViRt9nd1oXx0OuIF, -->
<#-- node 11629:21144): title + two body paragraphs + side-by-side No/Yes -->
<#-- buttons. The `deviceNameRequired` JS-prompt path is preserved so the -->
<#-- SPI's optional device-name flow still works when the operator enables it. -->
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header">
        ${msg("trusted-device-header")}
    <#elseif section = "form">
        <#if deviceNameRequired>
            <script>
                function inputName(e) {
                    const elem = document.querySelector("#kc-trusted-device-name");
                    const result = prompt("${msg("trusted-device-name")?js_string}", elem.value);
                    if (result === null) {
                        e.preventDefault();
                        return false;
                    }
                    elem.value = result;
                }
            </script>
        </#if>

        <form id="kc-form-trusted-device" class="${properties.kcFormClass!}"
              action="${url.loginAction}" method="post">
            <div class="tp-trusted-device-explanation">
                <p>${msg("trustedDeviceExplanationParagraph1")}</p>
                <p>${msg("trustedDeviceExplanationParagraph2")}</p>
            </div>

            <#if deviceNameRequired>
                <input type="hidden" id="kc-trusted-device-name" name="trusted-device-name" value="${trustedDeviceName!''}"/>
            </#if>

            <div class="tp-totp-actions">
                <button type="submit" name="trusted-device" value="no"
                        class="${properties.kcButtonDefaultClass!}" id="kc-trusted-device-no">${msg("trusted-device-no")}</button>
                <button type="submit" name="trusted-device" value="yes"
                        class="${properties.kcButtonPrimaryClass!}" id="kc-trusted-device-yes"
                        <#if deviceNameRequired>onclick="inputName(event)"</#if>>${msg("trusted-device-yes")}</button>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
