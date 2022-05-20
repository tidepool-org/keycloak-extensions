<#import "template.ftl" as layout>
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
      Hey there!
    <#elseif section = "content">
      ${kcSanitize(msg("identityProviderLinkBodyHtml", identityProviderAlias, realmName, identityProviderContext.username, link, linkExpiration, linkExpirationFormatter(linkExpiration)))?no_esc}
    <#elseif section = "actionText">
      Confirm Account Linking
    <#elseif section = "actionLink">
        ${kcSanitize(link)?no_esc}
    </#if>
</@layout.emailLayout>
