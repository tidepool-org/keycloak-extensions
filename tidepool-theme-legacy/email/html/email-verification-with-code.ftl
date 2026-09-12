<#import "template.ftl" as layout>
<#-- Tidepool change: switched to the sectioned emailLayout. displayAction=false because the code is meant to be -->
<#-- copy/pasted, not clicked — no CTA button. Body copy inlined in English. -->
<@layout.emailLayout displayHeader=true displayAction=false; section>
    <#if section = "header">
      Hey there!
    <#elseif section = "content">
      Please verify your email address by entering in the following code ${kcSanitize(code)?no_esc}.
    </#if>
</@layout.emailLayout>
