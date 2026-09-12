<#import "template.ftl" as layout>
<#import "tp-commons.ftl" as tp>
<@layout.registrationLayout displayInfo=false; section>
    <#if section = "header">
        ${msg("rolePromptTitle")}
    <#elseif section = "form">
        <form id="tp-role-prompt" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="tp-card-options tp-card-options--role">
                <label class="tp-card-option">
                    <input type="radio" name="role" value="patient" class="tp-card-option-input" />
                    <span class="tp-card-option-card">
                        <span class="tp-card-option-text">
                            <span class="tp-card-option-title">${msg("createAccountPersonal")}</span>
                            <span class="tp-card-option-description">${msg("rolePromptPersonalDescription")}</span>
                        </span>
                        <span class="tp-card-option-check" aria-hidden="true"><@tp.checkIcon/></span>
                    </span>
                </label>

                <label class="tp-card-option">
                    <input type="radio" name="role" value="clinician" class="tp-card-option-input" />
                    <span class="tp-card-option-card">
                        <span class="tp-card-option-text">
                            <span class="tp-card-option-title">${msg("createAccountClinician")}</span>
                            <span class="tp-card-option-description">${msg("rolePromptClinicianDescription")}</span>
                        </span>
                        <span class="tp-card-option-check" aria-hidden="true"><@tp.checkIcon/></span>
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
