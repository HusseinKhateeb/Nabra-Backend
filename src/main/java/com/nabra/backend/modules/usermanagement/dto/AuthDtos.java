package com.nabra.backend.modules.usermanagement.dto;

import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDtos {

  // =====================
  // REGISTER
  // =====================
  public record RegisterRequest(
      @NotBlank @Size(min = 3, max = 50) String username,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8, max = 80) String password,
      @NotBlank @Size(min = 2, max = 80) String displayName,
      @NotNull UserType userType
  ) {}

  // =====================
  // LOGIN
  // =====================
  public record LoginRequest(
      @NotBlank String username,
      @NotBlank String password
  ) {}

  // =====================
  // AUTH RESPONSE
  // =====================
  public record AuthResponse(
      String accessToken,
      String tokenType,
      String userId,
      String username,
      String email,
      String role,
      UserStatus status,
      boolean emailVerified
  ) {}

  // =====================
  // CHANGE PASSWORD (logged in)
  // =====================
  public record ChangePasswordRequest(
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 80) String newPassword,
      @NotBlank @Size(min = 8, max = 80) String confirmPassword
  ) {}

  // =====================
  // REFRESH TOKEN
  // =====================
  public record RefreshTokenRequest(
      @NotBlank String refreshToken
  ) {}

  // =====================
  // LOGOUT
  // =====================
  public record LogoutRequest(
      String refreshToken
  ) {}

  // ==================================================
  // 🔐 FORGOT PASSWORD FLOW
  // ==================================================

  // 1️⃣ إدخال الإيميل
  public record ForgotPasswordRequest(
      @NotBlank @Email String email
  ) {}

  // 2️⃣ التحقق من الكود
  public record VerifyResetCodeRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 6, max = 6) String code
  ) {}

  // 3️⃣ تعيين كلمة مرور جديدة
  public record ResetPasswordRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 6, max = 6) String code,
      @NotBlank @Size(min = 8, max = 80) String newPassword,
      @NotBlank @Size(min = 8, max = 80) String confirmPassword
  ) {}

  // ==================================================
  // 🔐 GOOGLE AUTH (NEW)
  // ==================================================
  public record GoogleAuthRequest(
      @NotBlank String idToken
  ) {}
}
