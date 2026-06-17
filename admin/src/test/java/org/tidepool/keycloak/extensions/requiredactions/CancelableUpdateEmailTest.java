package org.tidepool.keycloak.extensions.requiredactions;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelableUpdateEmailTest {

    private RequiredActionContext context;
    private AuthenticationSessionModel authSession;
    private UserModel user;
    private MultivaluedMap<String, String> formData;
    private CancelableUpdateEmail action;

    @BeforeEach
    void setUp() {
        context = mock(RequiredActionContext.class);
        authSession = mock(AuthenticationSessionModel.class);
        user = mock(UserModel.class);
        HttpRequest httpRequest = mock(HttpRequest.class);
        formData = new MultivaluedHashMap<>();

        lenient().when(context.getAuthenticationSession()).thenReturn(authSession);
        lenient().when(context.getUser()).thenReturn(user);
        lenient().when(context.getHttpRequest()).thenReturn(httpRequest);
        lenient().when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        lenient().when(user.getId()).thenReturn("user-1");

        action = new CancelableUpdateEmail();
    }

    private void markEnforced() {
        when(authSession.getClientNote(Constants.KC_ACTION_ENFORCED)).thenReturn(Boolean.TRUE.toString());
    }

    @Test
    void cancellingEnforcedActionClearsPendingEmailAndRequiredAction() {
        markEnforced();
        formData.putSingle(CancelableUpdateEmail.CANCEL_PARAMETER, "true");

        action.processAction(context);

        verify(user).removeAttribute(UserModel.EMAIL_PENDING);
        verify(user).removeRequiredAction(UserModel.RequiredAction.UPDATE_EMAIL);
        verify(context).success();
    }

    @Test
    void enforcedDetectionReflectsClientNote() {
        // No note -> not enforced.
        lenient().when(authSession.getClientNote(Constants.KC_ACTION_ENFORCED)).thenReturn(null);
        assertThat(CancelableUpdateEmail.isEnforcedAppInitiatedAction(context)).isFalse();

        when(authSession.getClientNote(Constants.KC_ACTION_ENFORCED)).thenReturn("false");
        assertThat(CancelableUpdateEmail.isEnforcedAppInitiatedAction(context)).isFalse();

        markEnforced();
        assertThat(CancelableUpdateEmail.isEnforcedAppInitiatedAction(context)).isTrue();
    }

    @Test
    void enforcedDetectionFalseWithoutAuthSession() {
        when(context.getAuthenticationSession()).thenReturn(null);
        assertThat(CancelableUpdateEmail.isEnforcedAppInitiatedAction(context)).isFalse();
    }
}
