package com.nabra.backend.modules.usermanagement.dto;

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
      @NotNull UserType userType,
      String preferredLanguage
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
      String role,
      String preferredLanguage
  ) {}
}
