<#--
  Tidepool override of the keycloak-home-idp-discovery plugin's
  theme-resources/templates/hidpd-select-idp.ftl. A theme's own login/<name>.ftl
  takes precedence over a theme-resources templates/<name>.ftl, so this file is
  used instead of the one bundled in the plugin jar — keeping all design changes
  in this repository rather than in the upstream submodule.

  Functional logic (the hidpd.providers guard, loop, loginUrl and title) is
  preserved from the upstream template. Each provider is rendered as a card
  (.tp-idp-option*, styled in css/tidepool.css) matching the
  authenticator-selection screen, with a single Flaticon UIcons font glyph
  (subset in fonts/uicons/) — change the glyph via .tp-idp-option-icon i::before.
-->
<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "socialProviders">
        <#-- providers are rendered in the form section (below) so they appear above the "try another way" button -->
    <#elseif section = "form">
        <#if realm.password && hidpd.providers?? && hidpd.providers?has_content>
            <div class="pf-v5-c-login__main-footer-band">
                <p class="pf-v5-c-login__main-footer-band-item" id="hidpd-select-provider-title">
                    ${msg("home-idp-discovery-identity-provider-login-label")}
                </p>
            </div>
            <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                <ul class="tp-idp-options">
                    <#list hidpd.providers as p>
                        <li class="tp-idp-option-item">
                            <a id="social-${p.alias}" class="tp-idp-option" aria-label="${p.displayName}" href="${p.loginUrl}">
                                <span class="tp-idp-option-icon" aria-hidden="true"><i></i></span>
                                <span class="tp-idp-option-title">${p.displayName!}</span>
                            </a>
                        </li>
                    </#list>
                </ul>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
