package com.nabra.backend.modules.usermanagement.service;

import com.nabra.backend.modules.usermanagement.dto.UserDtos;
import com.nabra.backend.modules.usermanagement.exception.UserNotFoundException;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import com.nabra.backend.modules.sessionhistory.repository.SessionRepository;
import com.nabra.backend.modules.chatcommunication.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user profile and user management operations.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final SessionRepository sessionRepository;
  private final ChatRepository chatRepository;

  public User getById(String userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
  }

  /** ✅ جديد: جلب كل المستخدمين */
  public List<User> findAll() {
    return userRepository.findAll();
  }

  public UserDtos.UserProfileResponse toProfile(User u) {
    // حساب الإحصائيات من Database
    long sessionsCount = sessionRepository.countSessionsByUserId(u.getId());
    long chatsCount = chatRepository.countChatsByUserId(u.getId());
    double accuracy = u.getAccuracy() != null ? u.getAccuracy() : 0.0;

    UserDtos.UserStatistics statistics = new UserDtos.UserStatistics(
        sessionsCount,    // عدد الجلسات من DB
        chatsCount,        // عدد المحادثات من DB
        accuracy           // النسبة من User entity
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

  @Transactional
  public UserDtos.UserProfileResponse updateProfile(String userId, UserDtos.UpdateProfileRequest req) {
    User u = getById(userId);

    if (req.displayName() != null) u.setDisplayName(req.displayName());
    if (req.gender() != null) u.setGender(req.gender());
    if (req.age() != null) u.setAge(req.age());
    if (req.avatarUrl() != null) u.setAvatarUrl(req.avatarUrl());
    if (req.phoneNumber() != null) u.setPhoneNumber(req.phoneNumber());
    if (req.highContrastEnabled() != null) u.setHighContrastEnabled(req.highContrastEnabled());
    if (req.fontScale() != null) u.setFontScale(req.fontScale());
    if (req.vibrationEnabled() != null) u.setVibrationEnabled(req.vibrationEnabled());

    userRepository.save(u);
    return toProfile(u);
  }

  /** ✅ جديد: Get User Profile مع الإحصائيات من DB */
  public UserDtos.UserProfileResponse getProfile(String userId) {
    return toProfile(getById(userId));
  }

  /** ✅ جديد: Get User Settings */
  public UserDtos.UserSettingsResponse getSettings(String userId) {
    User user = getById(userId);

    return new UserDtos.UserSettingsResponse(
        user.getId(),
        user.getPreferredLanguage(),
        user.isHighContrastEnabled(),
        user.getFontScale(),
        user.isVibrationEnabled(),
        user.isPublicProfile(),
        user.isShowContactInfo(),
        user.isAllowNotifications(),
        user.isAllowDataCollection(),
        user.getDataCollectionLevel()
    );
  }

  /** ✅ جديد: Update User Settings */
  @Transactional
  public UserDtos.UserSettingsResponse updateSettings(String userId, UserDtos.UpdateSettingsRequest req) {
    User user = getById(userId);

    if (req.preferredLanguage() != null) user.setPreferredLanguage(req.preferredLanguage());
    if (req.highContrastEnabled() != null) user.setHighContrastEnabled(req.highContrastEnabled());
    if (req.fontScale() != null) user.setFontScale(req.fontScale());
    if (req.vibrationEnabled() != null) user.setVibrationEnabled(req.vibrationEnabled());
    if (req.publicProfile() != null) user.setPublicProfile(req.publicProfile());
    if (req.showContactInfo() != null) user.setShowContactInfo(req.showContactInfo());
    if (req.allowNotifications() != null) user.setAllowNotifications(req.allowNotifications());
    if (req.allowDataCollection() != null) user.setAllowDataCollection(req.allowDataCollection());
    if (req.dataCollectionLevel() != null) user.setDataCollectionLevel(req.dataCollectionLevel());

    userRepository.save(user);
    return getSettings(userId);
  }
}
