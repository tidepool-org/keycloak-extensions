<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=true; section>
    <#if section = "pre-header">
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
          <div id="kc-registration-container">
            <div id="kc-registration">
              <span><a tabindex="6" href="${url.registrationUrl}"><i class="fa fa-plus-circle"></i> ${msg("doRegister")}</a></span>
            </div>
          </div>
        </#if>
<#--    <#elseif section = "header">-->
<#--        <a href="${properties.tidepoolUrl!}">-->
<#--          <img class="logo" src="${url.resourcesPath}/img/tidepool-logo-880x96.png" alt="Tidepool"/>-->
<#--        </a>-->
    <#elseif section = "form">
    <div id="kc-form">
      <div id="kc-form-wrapper">
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <div class="${properties.kcFormGroupClass!}">
                    <#if usernameEditDisabled??>
                        <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}" type="text" placeholder="<#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>" disabled />
                    <#else>
                        <input tabindex="1" id="username" class="${properties.kcInputClass!}" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="off" placeholder="<#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>"
                               aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                        />

                        <#if messagesPerField.existsError('username','password')>
                            <span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                            </span>
                        </#if>
                    </#if>
                </div>

                <div class="${properties.kcFormGroupClass!}">
                    <input tabindex="2" id="password" class="${properties.kcInputClass!}" name="password" type="password" autocomplete="off" placeholder="${msg("password")}"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                    />
                </div>

                <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                    <div id="kc-form-options">
                        <#if realm.rememberMe && !usernameEditDisabled??>
                            <div class="checkbox">
                                <label>
                                    <#if login.rememberMe??>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                                    <#else>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                                    </#if>
                                </label>
                            </div>
                        </#if>
                        </div>
                        <div class="${properties.kcFormOptionsWrapperClass!}">
                            <#if realm.resetPasswordAllowed>
                                <span><a tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                            </#if>
                        </div>

                  </div>

                  <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                      <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                      <input tabindex="4" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                  </div>
            </form>
        </#if>
        </div>

        <#if realm.password && social.providers??>
            <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
                <hr/>
                <h4>${msg("identity-provider-login-label")}</h4>

                <ul class="${properties.kcFormSocialAccountListClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountListGridClass!}</#if>">
                    <#list social.providers as p>
                        <a id="social-${p.alias}" class="${properties.kcFormSocialAccountListButtonClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
                                type="button" href="${p.loginUrl}">
                            <#if p.iconClasses?has_content>
                                <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
                                <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                            <#else>
                                <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                            </#if>
                        </a>
                    </#list>
                </ul>
            </div>
        </#if>

    </div>
    <#elseif section = "footer" >
      <div class="footer-section footer-section-top">
        <div class="footer-link social-media large-format-only">
          <a class="footer-twitter" href="https://twitter.com/tidepool_org" id="twitter" target="_blank" rel="noreferrer noopener">
            <svg viewBox="0 0 49 35"><path d="M43.94 9.783c0-.376 0-.75-.02-1.127 1.957-1.23 3.66-2.783 5.012-4.54-1.802.7-3.74 1.16-5.777 1.382 2.076-1.076 3.662-2.8 4.426-4.85-1.938 1.008-4.092 1.725-6.383 2.118C39.357 1.06 36.753 0 33.874 0 28.334 0 23.83 3.927 23.83 8.76c0 .68.097 1.347.254 1.996-8.34-.358-15.743-3.858-20.697-9.15-.86 1.296-1.35 2.8-1.35 4.404 0 3.04 1.78 5.72 4.464 7.29-1.644-.05-3.19-.444-4.542-1.093v.12c0 4.234 3.466 7.785 8.048 8.588-.842.205-1.723.307-2.644.307-.646 0-1.272-.05-1.88-.154 1.273 3.483 4.994 6.01 9.38 6.078-3.447 2.356-7.774 3.756-12.473 3.756-.804 0-1.607-.034-2.39-.12 4.425 2.46 9.712 3.91 15.37 3.91 18.465 0 28.57-13.35 28.57-24.91z"></path></svg>
          </a>
          <a class="footer-facebook" href="https://www.facebook.com/TidepoolOrg" id="facebook" target="_blank" rel="noreferrer noopener">
            <svg viewBox="0 0 32 32"><path d="M18,32V18h6l1-6h-7V9c0-2,1.002-3,3-3h3V0c-1,0-3.24,0-5,0c-5,0-7,3-7,8v4H6v6h6v14H18z"></path></svg>
          </a>
        </div>
        <div class="footer-link secondary large-format-only">
          <a href="http://tidepool.org/products/tidepool-mobile/" id="mobile" target="_blank" rel="noreferrer noopener">Get Mobile App</a>
          <a href="http://support.tidepool.org/" id="support" target="_blank" rel="noreferrer noopener">Get Support</a>
          <a href="http://tidepool.org/legal/" id="legal" target="_blank" rel="noreferrer noopener">Privacy and Terms of Use</a>
        </div>
        <div class="footer-link footer-jdrf">
          <a href="http://jdrf.org/" id="jdrf" target="_blank" rel="noreferrer noopener">
            Made possible by<div class="jdrf-logo-container">
              <img src="${url.resourcesPath}/img/jdrf_hover.png" alt="JDRF" class="jdrf-logo-hover">
              <img src="${url.resourcesPath}/img/jdrf.png" alt="JDRF" class="jdrf-logo">
            </div>
          </a>
        </div>
      </div>
    </#if>

</@layout.registrationLayout>
