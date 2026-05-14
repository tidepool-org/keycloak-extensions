<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header">
        ${msg("rolePromptTitle")}
    <#elseif section = "form">
        <form id="tp-role-prompt" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="tp-role-options">
                <label class="tp-role-option">
                    <input type="radio" name="role" value="patient" class="tp-role-option-input" />
                    <span class="tp-role-option-card">
                        <span class="tp-role-option-text">
                            <span class="tp-role-option-title">${msg("createAccountPersonal")}</span>
                            <span class="tp-role-option-description">${msg("rolePromptPersonalDescription")}</span>
                        </span>
                        <span class="tp-role-option-check" aria-hidden="true">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                        </span>
                    </span>
                </label>

                <label class="tp-role-option">
                    <input type="radio" name="role" value="clinician" class="tp-role-option-input" />
                    <span class="tp-role-option-card">
                        <span class="tp-role-option-text">
                            <span class="tp-role-option-title">${msg("createAccountClinician")}</span>
                            <span class="tp-role-option-description">${msg("rolePromptClinicianDescription")}</span>
                        </span>
                        <span class="tp-role-option-check" aria-hidden="true">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                        </span>
                    </span>
                </label>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcFormActionGroupClass!}">
                    <button id="continue" class="${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!}" type="submit" disabled>${msg("next")}</button>
                </div>
            </div>
        </form>
        <script>
            document.querySelectorAll('input[name="role"]').forEach(function (r) {
                r.addEventListener('change', function () {
                    document.getElementById('continue').disabled = false;
                });
            });
        </script>
    </#if>
</@layout.registrationLayout>
