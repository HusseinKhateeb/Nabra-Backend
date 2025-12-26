package com.nabra.backend.modules.usermanagement.service;

import com.nabra.backend.modules.usermanagement.dto.UserDtos;
import com.nabra.backend.modules.usermanagement.exception.UserNotFoundException;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user profile and user management operations.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  /**
   * Get user by ID.
   * @param userId User ID
   * @return User entity
   * @throws UserNotFoundException if user not found
   */
  public User getById(String userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
  }

  /**
   * Convert User entity to UserProfileResponse DTO.
   * @param u User entity
   * @return User profile response
   */
  public UserDtos.UserProfileResponse toProfile(User u) {
    UserDtos.UserStatistics statistics = new UserDtos.UserStatistics(
        u.getTotalTransfers(),
        u.getHoursOfUse(),
        u.getAccuracy()
    );
    
    return new UserDtos.UserProfileResponse(
        u.getId(),
        u.getUsername(),
        u.getDisplayName(),
        u.getEmail(),
        u.getPhoneNumber(),
        u.getCreatedAt(),
        u.getRole(),
        u.getStatus(),
        u.getUserType(),
        u.getGender(),
        u.getAge(),
        u.getAvatarUrl(),
        u.isHighContrastEnabled(),
        u.getFontScale(),
        u.isVibrationEnabled(),
        u.isEmailVerified(),
        statistics
    );
  }

  /**
   * Update user profile information.
   * @param userId User ID
   * @param req Profile update request
   * @return Updated user profile
   * @throws UserNotFoundException if user not found
   */
  @Transactional
  public UserDtos.UserProfileResponse updateProfile(String userId, UserDtos.UpdateProfileRequest req) {
    User u = getById(userId);
    
    if (req.displayName() != null) {
      u.setDisplayName(req.displayName());
    }
    if (req.gender() != null) {
      u.setGender(req.gender());
    }
    if (req.age() != null) {
      u.setAge(req.age());
    }
    if (req.avatarUrl() != null) {
      u.setAvatarUrl(req.avatarUrl());
    }
    if (req.phoneNumber() != null) {
      u.setPhoneNumber(req.phoneNumber());
    }
    if (req.highContrastEnabled() != null) {
      u.setHighContrastEnabled(req.highContrastEnabled());
    }
    if (req.fontScale() != null) {
      u.setFontScale(req.fontScale());
    }
    if (req.vibrationEnabled() != null) {
      u.setVibrationEnabled(req.vibrationEnabled());
    }
    
    userRepository.save(u);
    return toProfile(u);
  }
}
