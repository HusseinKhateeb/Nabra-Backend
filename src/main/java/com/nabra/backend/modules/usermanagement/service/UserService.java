package com.nabra.backend.modules.usermanagement.service;

import com.nabra.backend.modules.usermanagement.dto.UserDtos;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public User getById(String userId) {
    return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  public UserDtos.UserProfileResponse toProfile(User u) {
    return new UserDtos.UserProfileResponse(
        u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName(), u.getGender(), u.getAge(), u.getAvatarUrl(),
        u.getRole(), u.getUserType(), u.getPreferredLanguage(), u.isHighContrastEnabled(), u.getFontScale(), u.isVibrationEnabled()
    );
  }

  public UserDtos.UserProfileResponse updateProfile(String userId, UserDtos.UpdateProfileRequest req) {
    User u = getById(userId);
    if (req.displayName() != null) u.setDisplayName(req.displayName());
    if (req.gender() != null) u.setGender(req.gender());
    if (req.age() != null) u.setAge(req.age());
    if (req.avatarUrl() != null) u.setAvatarUrl(req.avatarUrl());
    if (req.preferredLanguage() != null) u.setPreferredLanguage(req.preferredLanguage());
    if (req.highContrastEnabled() != null) u.setHighContrastEnabled(req.highContrastEnabled());
    if (req.fontScale() != null) u.setFontScale(req.fontScale());
    if (req.vibrationEnabled() != null) u.setVibrationEnabled(req.vibrationEnabled());
    userRepository.save(u);
    return toProfile(u);
  }
}
