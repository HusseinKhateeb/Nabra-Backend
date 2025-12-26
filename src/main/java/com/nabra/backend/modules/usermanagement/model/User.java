package com.nabra.backend.modules.usermanagement.model;

import com.nabra.backend.common.model.BaseEntity;
import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username", unique = true),
    @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
public class User extends BaseEntity {

  @Column(nullable = false, length = 50, unique = true)
  private String username;

  @Column(nullable = false, length = 120, unique = true)
  private String email;

  /** BCrypt hash. */
  @Column(nullable = false, length = 120)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role = UserRole.USER;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserStatus status = UserStatus.ACTIVE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserType userType = UserType.OTHER;

  @Column(nullable = false, length = 80)
  private String displayName;

  @Column(length = 20)
  private String gender;

  private Integer age;

  @Column(length = 400)
  private String avatarUrl;

  /** e.g., "ar" / "en" */
  @Column(nullable = false, length = 10)
  private String preferredLanguage = "ar";

  @Column(nullable = false)
  private boolean highContrastEnabled = false;

  /** 1.0 = default. */
  @Column(nullable = false)
  private double fontScale = 1.0;

  @Column(nullable = false)
  private boolean vibrationEnabled = true;

  /** Email verification flag */
  @Column(nullable = false)
  private boolean emailVerified = false;

  /** Last login timestamp for audit purposes */
  @Column
  private Instant lastLogin;

  /** User phone number */
  @Column(length = 20)
  private String phoneNumber;

  /** Total number of transfers */
  @Column
  private long totalTransfers = 0;

  /** Total hours of platform usage */
  @Column
  private double hoursOfUse = 0.0;

  /** Accuracy percentage (0-100) */
  @Column
  private double accuracy = 0.0;

  /**
   * Blocking relation: if A blocks B, then B should not be able to message A.
   */
  @ManyToMany
  @JoinTable(
      name = "user_blocks",
      joinColumns = @JoinColumn(name = "blocker_id"),
      inverseJoinColumns = @JoinColumn(name = "blocked_id")
  )
  private Set<User> blockedUsers = new HashSet<>();
}
