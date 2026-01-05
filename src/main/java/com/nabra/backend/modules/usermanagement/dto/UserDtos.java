package com.nabra.backend.modules.usermanagement.dto;

import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class UserDtos {

  /** 🔹 DTO بسيط لقائمة المستخدمين (للشات) */
  public record UserListItem(
      String id,
      String displayName,
      String avatarUrl
  ) {}

  /** Statistics for user profile */
  public record UserStatistics(
      long transfers,
      double hoursOfUse,
      double accuracy
  ) {}

  public record UserProfileResponse(
      String id,
      String username,
      String displayName,
      String email,
      String phoneNumber,
      Instant joinDate,
      UserRole role,
      UserStatus status,
      UserType userType,
      String gender,
      Integer age,
      String avatarUrl,
      boolean highContrastEnabled,
      double fontScale,
      boolean vibrationEnabled,
      boolean emailVerified,
      UserStatistics statistics
  ) {}

  public record UpdateProfileRequest(
      @Size(min = 2, max = 80) String displayName,
      String gender,
      @Min(1) @Max(120) Integer age,
      @Size(max = 400) String avatarUrl,
      @Size(max = 20) String phoneNumber,
      Boolean highContrastEnabled,
      Double fontScale,
      Boolean vibrationEnabled
  ) {}
}
