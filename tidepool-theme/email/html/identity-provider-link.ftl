<#import "template.ftl" as layout>
<#-- Tidepool change: switched to the sectioned emailLayout (header / content / actionText / actionLink) so the -->
<#-- link is rendered as a Tidepool-styled CTA button. -->
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
      Hey there!
    <#elseif section = "content">
      ${kcSanitize(msg("identityProviderLinkBodyHtml", identityProviderDisplayName, realmName, identityProviderContext.username, link, linkExpiration, linkExpirationFormatter(linkExpiration)))?no_esc}
    <#elseif section = "actionText">
      Confirm Account Linking
    <#elseif section = "actionLink">${kcSanitize(link)?no_esc}</#if>
</@layout.emailLayout>
