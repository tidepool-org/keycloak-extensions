<#outputformat "plainText">
<#assign requiredActionsText><#if requiredActions??><#list requiredActions><#items as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#sep></#items></#list></#if></#assign>
</#outputformat>

<#import "template.ftl" as layout>
<#-- Tidepool change: switched to the sectioned emailLayout — body kept as the upstream msg() call, but the action -->
<#-- link is broken out into actionLink/actionText slots so it renders as the Tidepool CTA button. -->
<@layout.emailLayout displayHeader=true displayAction=true; section>
    <#if section = "header">
      Hey there!
    <#elseif section = "content">
      ${kcSanitize(msg("executeActionsBodyHtml",link, linkExpiration, realmName, requiredActionsText, linkExpirationFormatter(linkExpiration)))?no_esc}
    <#elseif section = "actionText">
      Update Your Account
    <#elseif section = "actionLink">${kcSanitize(link)?no_esc}</#if>
</@layout.emailLayout>
