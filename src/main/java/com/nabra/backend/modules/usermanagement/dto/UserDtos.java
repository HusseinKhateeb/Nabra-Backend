package com.nabra.backend.modules.usermanagement.dto;

import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class UserDtos {

  public record UserProfileResponse(
      String id,
      String username,
      String email,
      String displayName,
      String gender,
      Integer age,
      String avatarUrl,
      UserRole role,
      UserType userType,
      String preferredLanguage,
      boolean highContrastEnabled,
      double fontScale,
      boolean vibrationEnabled
  ) {}

  public record UpdateProfileRequest(
      @Size(min = 2, max = 80) String displayName,
      String gender,
      @Min(1) @Max(120) Integer age,
      @Size(max = 400) String avatarUrl,
      @Size(max = 10) String preferredLanguage,
      Boolean highContrastEnabled,
      Double fontScale,
      Boolean vibrationEnabled
  ) {}
}
