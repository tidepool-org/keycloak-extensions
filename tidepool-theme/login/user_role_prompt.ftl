<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "form">
        <form id="tp-role-prompt" action="${url.loginAction}" method="post">
            <div class="role-prompt-container">
                <h2 class="role-prompt-title">Which kind of account do you need?</h2>
                <label onclick="document.getElementById('continue').disabled = false">
                    <input type="radio" name="role" class="card-input-element" value="patient"/>
                    <div class="card-input">
                        <div class="card-input-header">
                            <div class="card-input-title">Personal Account</div>
                            <div class="card-input-check"><img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzNSIgaGVpZ2h0PSIyNyIgdmlld0JveD0iMCAwIDM1IDI3Ij4KICAgIDxwYXRoIGZpbGw9Im5vbmUiIGZpbGwtcnVsZT0iZXZlbm9kZCIgc3Ryb2tlPSIjNjA3OEZGIiBzdHJva2Utd2lkdGg9IjYiIGQ9Ik0zIDEyLjY3NGw5LjY2MyA5LjY2M0wzMiAzIi8+Cjwvc3ZnPgo="></div>
                        </div>
                        <div class="card-input-description">
                            You want to manage your diabetes data. You are caring for or supporting someone with diabetes.
                        </div>
                    </div>
                </label>
                <label onclick="document.getElementById('continue').disabled = false">
                    <input type="radio" name="role" class="card-input-element" value="clinician"/>
                    <div class="card-input">
                        <div class="card-input-header">
                            <div class="card-input-title">Clinical Account</div>
                            <div class="card-input-check"><img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzNSIgaGVpZ2h0PSIyNyIgdmlld0JveD0iMCAwIDM1IDI3Ij4KICAgIDxwYXRoIGZpbGw9Im5vbmUiIGZpbGwtcnVsZT0iZXZlbm9kZCIgc3Ryb2tlPSIjNjA3OEZGIiBzdHJva2Utd2lkdGg9IjYiIGQ9Ik0zIDEyLjY3NGw5LjY2MyA5LjY2M0wzMiAzIi8+Cjwvc3ZnPgo="></div>
                        </div>
                        <div class="card-input-description">
                            You are a doctor, nurse, or other clinical or administrative staff who wants to use Tidepool at your clinic.
                        </div>
                    </div>
                </label>

                <div class="role-prompt-continue">
                    <input id="continue" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}" type="submit" value="${msg("continue")}" disabled/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>