<div id="kc-form">
  <div id="kc-form-wrapper">
    <div class="${properties.kcFormGroupClass!}">
      <label for="username"
             class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

      <div class="attempted-username">
        <input disabled
               id="username"
               class="${properties.kcInputClass!}"
               type="text" autocomplete="off"
               value="${auth.attemptedUsername}"
        />
        <a class="embedded-link" id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("notYou")}">${msg("notYou")}</a>
      </div>
    </div>
  </div>
</div>