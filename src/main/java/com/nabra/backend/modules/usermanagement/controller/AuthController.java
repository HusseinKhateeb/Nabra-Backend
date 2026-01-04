package com.nabra.backend.modules.usermanagement.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.usermanagement.dto.AuthDtos;
import com.nabra.backend.modules.usermanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication")
public class AuthController {

  private final AuthService authService;
  private final SecurityUtils securityUtils;

  // ============================
  // REGISTER
  // ============================
  @PostMapping("/register")
  public ResponseEntity<AuthDtos.AuthResponse> register(
      @Valid @RequestBody AuthDtos.RegisterRequest req
  ) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(authService.register(req));
  }

  // ============================
  // LOGIN
  // ============================
  @PostMapping("/login")
  public ResponseEntity<AuthDtos.AuthResponse> login(
      @Valid @RequestBody AuthDtos.LoginRequest req
  ) {
    return ResponseEntity.ok(authService.login(req));
  }

  // ============================
  // CHANGE PASSWORD
  // ============================
  @PostMapping("/change-password")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> changePassword(
      @Valid @RequestBody AuthDtos.ChangePasswordRequest req
  ) {
    authService.changePassword(securityUtils.getCurrentUserId(), req);
    return ResponseEntity.ok().build();
  }

  // ============================
  // FORGOT PASSWORD
  // ============================
  @PostMapping("/forgot-password")
  public ResponseEntity<Void> forgotPassword(
      @Valid @RequestBody AuthDtos.ForgotPasswordRequest req
  ) {
    authService.forgotPassword(req);
    return ResponseEntity.ok().build();
  }

  // ============================
  // VERIFY RESET CODE
  // ============================
  @PostMapping("/verify-reset-code")
  public ResponseEntity<Void> verifyResetCode(
      @Valid @RequestBody AuthDtos.VerifyResetCodeRequest req
  ) {
    authService.verifyResetCode(req);
    return ResponseEntity.ok().build();
  }

  // ============================
  // RESET PASSWORD
  // ============================
  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(
      @Valid @RequestBody AuthDtos.ResetPasswordRequest req
  ) {
    authService.resetPassword(req);
    return ResponseEntity.ok().build();
  }
  // ============================
// GOOGLE AUTH
// ============================
@PostMapping("/google")
public ResponseEntity<AuthDtos.AuthResponse> googleAuth(
    @Valid @RequestBody AuthDtos.GoogleAuthRequest req
) {
    return ResponseEntity.ok(authService.googleAuth(req));
}

}
