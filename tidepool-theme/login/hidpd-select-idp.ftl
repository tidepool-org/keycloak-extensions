<#--
  Tidepool override of the keycloak-home-idp-discovery plugin's
  theme-resources/templates/hidpd-select-idp.ftl. A theme's own login/<name>.ftl
  takes precedence over a theme-resources templates/<name>.ftl, so this file is
  used instead of the one bundled in the plugin jar — keeping all design changes
  in this repository rather than in the upstream submodule.

  Functional logic (the hidpd.providers guard, loop, loginUrl) is preserved from
  upstream. Design per Figma file g8xYrHViRt9nd1oXx0OuIF, node 13885:30576: each
  provider is a radio-selectable card (.tp-card-option*, styled in css/tidepool.css)
  with a Flaticon UIcons font glyph (subset in fonts/uicons/ — change the glyph via
  .tp-card-option-icon i::before). Unlike upstream's direct links, the user picks a
  provider and a Next button navigates to its loginUrl client-side.
-->
<#import "template.ftl" as layout>
<#import "tp-commons.ftl" as tp>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??) subtitle=msg("hidpdSelectIdpSubtitle"); section>
    <#if section = "header">
        ${msg("hidpdSelectIdpTitle")}
    <#elseif section = "socialProviders">
        <#-- providers are rendered in the form section (below) so they appear above the "try another way" button -->
    <#elseif section = "form">
        <#if realm.password && hidpd.providers?? && hidpd.providers?has_content>
            <form id="kc-idp-select-form" class="${properties.kcFormClass!}" onsubmit="return false;">
                <div class="tp-card-options" role="radiogroup" aria-label="${msg("hidpdSelectIdpTitle")}">
                    <#list hidpd.providers as p>
                        <label class="tp-card-option">
                            <input type="radio" name="idpSelection" id="idp-${p.alias}"
                                   class="tp-card-option-input" value="${p.loginUrl}"/>
                            <span class="tp-card-option-card">
                                <span class="tp-card-option-icon" aria-hidden="true"><i></i></span>
                                <span class="tp-card-option-text"><span class="tp-card-option-title">${p.displayName!}</span></span>
                                <span class="tp-card-option-check" aria-hidden="true"><@tp.checkIcon/></span>
                            </span>
                        </label>
                    </#list>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <div class="${properties.kcFormActionGroupClass!}">
                        <button id="kc-idp-next" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" type="button" disabled>${msg("next")}</button>
                    </div>
                </div>
            </form>

            <@tp.otpDisclaimer showImportant=false/>

            <script>
                (function () {
                    var next = document.getElementById('kc-idp-next');
                    var selectedUrl = null;
                    document.querySelectorAll('input[name="idpSelection"]').forEach(function (radio) {
                        radio.addEventListener('change', function () {
                            selectedUrl = this.value;
                            next.disabled = false;
                        });
                    });
                    next.addEventListener('click', function () {
                        if (selectedUrl) {
                            window.location.href = selectedUrl;
                        }
                    });
                })();
            </script>
        </#if>
    </#if>
</@layout.registrationLayout>
