package com.nabra.backend.modules.usermanagement.controller;

import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.usermanagement.dto.AuthDtos;
import com.nabra.backend.modules.usermanagement.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController controller;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, mock(com.nabra.backend.common.web.SecurityUtils.class));
    }

    @AfterEach
    void tearDown() {
        if (securityUtils != null) {
            securityUtils.close();
        }
    }

    @Test
    void register_shouldReturnCreated() {
        var req = new AuthDtos.RegisterRequest("testuser", "test@test.com", "password123", "Test User", UserType.OTHER);
        var expected = new AuthDtos.AuthResponse("token", "Bearer", "u1", "testuser", "test@test.com", "USER", UserStatus.ACTIVE, false);
        when(authService.register(any())).thenReturn(expected);

        ResponseEntity<AuthDtos.AuthResponse> response = controller.register(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(authService).register(req);
    }

    @Test
    void login_shouldReturnOk() {
        var req = new AuthDtos.LoginRequest("testuser", "password123");
        var expected = new AuthDtos.AuthResponse("token", "Bearer", "u1", "testuser", "test@test.com", "USER", UserStatus.ACTIVE, false);
        when(authService.login(any())).thenReturn(expected);

        ResponseEntity<AuthDtos.AuthResponse> response = controller.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(authService).login(req);
    }

    @Test
    void changePassword_shouldReturnOk() {
        var req = new AuthDtos.ChangePasswordRequest("oldPass", "newPass123", "newPass123");
        try (var securityUtilsStatic = mockStatic(SecurityUtils.class)) {
            securityUtilsStatic.when(SecurityUtils::getCurrentUserId).thenReturn("u1");

            ResponseEntity<Void> response = controller.changePassword(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(authService).changePassword("u1", req);
        }
    }

    @Test
    void forgotPassword_shouldReturnOk() {
        var req = new AuthDtos.ForgotPasswordRequest("test@test.com");

        ResponseEntity<Void> response = controller.forgotPassword(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).forgotPassword(req);
    }

    @Test
    void verifyResetCode_shouldReturnOk() {
        var req = new AuthDtos.VerifyResetCodeRequest("test@test.com", "123456");

        ResponseEntity<Void> response = controller.verifyResetCode(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).verifyResetCode(req);
    }

    @Test
    void resetPassword_shouldReturnOk() {
        var req = new AuthDtos.ResetPasswordRequest("test@test.com", "123456", "newPass123", "newPass123");

        ResponseEntity<Void> response = controller.resetPassword(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authService).resetPassword(req);
    }

    @Test
    void googleAuth_shouldReturnOk() {
        var req = new AuthDtos.GoogleAuthRequest("google-id-token");
        var expected = new AuthDtos.AuthResponse("token", "Bearer", "u1", "googleuser", "google@test.com", "USER", UserStatus.ACTIVE, true);
        when(authService.googleAuth(any())).thenReturn(expected);

        ResponseEntity<AuthDtos.AuthResponse> response = controller.googleAuth(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(authService).googleAuth(req);
    }
}
