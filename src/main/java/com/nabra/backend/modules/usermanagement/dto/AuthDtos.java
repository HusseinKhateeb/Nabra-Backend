package com.nabra.backend.modules.usermanagement.dto;

import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AuthDtos {

  public record RegisterRequest(
      @NotBlank @Size(min = 3, max = 50) String username,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8, max = 80) String password,
      @NotBlank @Size(min = 2, max = 80) String displayName,
      @NotNull UserType userType
  ) {}

  public record LoginRequest(
      @NotBlank String username,
      @NotBlank String password
  ) {}

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

  public record ChangePasswordRequest(
      @NotBlank String currentPassword,
      @NotBlank @Size(min = 8, max = 80) String newPassword,
      @NotBlank @Size(min = 8, max = 80) String confirmPassword
  ) {}

  public record RefreshTokenRequest(
      @NotBlank String refreshToken
  ) {}

  public record LogoutRequest(
      String refreshToken
  ) {}
}
