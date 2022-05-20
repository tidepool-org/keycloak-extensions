<#import "template.ftl" as layout>
<@layout.emailLayout displayHeader=true displayAction=false; section>
    <#if section = "header">
      Hey there!
    <#elseif section = "content">
      Please verify your email address by entering in the following code ${kcSanitize(code)?no_esc}.
    </#if>
</@layout.emailLayout>
