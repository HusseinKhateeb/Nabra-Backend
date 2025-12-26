package com.nabra.backend.modules.usermanagement.service;

import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.modules.usermanagement.dto.AuthDtos;
import com.nabra.backend.modules.usermanagement.exception.InvalidCredentialsException;
import com.nabra.backend.modules.usermanagement.exception.UserAlreadyExistsException;
import com.nabra.backend.modules.usermanagement.exception.UserNotFoundException;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import com.nabra.backend.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for handling user authentication and authorization.
 * Manages login, registration, password changes, and token operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  /**
   * Register a new user with validation.
   *
   * @param req Registration request containing username, email, password, displayName, userType
   * @return AuthResponse with JWT access token
   * @throws UserAlreadyExistsException if username or email already exists
   */
  @Transactional
  public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
    log.info("Registering new user with username: {}", req.username());

    // Validate username uniqueness
    if (userRepository.existsByUsername(req.username())) {
      log.warn("Registration failed: Username already exists: {}", req.username());
      throw new UserAlreadyExistsException("Username '" + req.username() + "' is already taken");
    }

    // Validate email uniqueness
    if (userRepository.existsByEmail(req.email())) {
      log.warn("Registration failed: Email already exists: {}", req.email());
      throw new UserAlreadyExistsException("Email '" + req.email() + "' is already registered");
    }

    // Create new user
    User user = new User();
    user.setUsername(req.username());
    user.setEmail(req.email());
    user.setDisplayName(req.displayName());
    user.setUserType(req.userType());
    user.setPasswordHash(passwordEncoder.encode(req.password()));
    user.setRole(UserRole.USER);
    user.setStatus(UserStatus.ACTIVE);
    user.setEmailVerified(false); // Email verification can be implemented later

    User savedUser = userRepository.save(user);
    log.info("User registered successfully with id: {}", savedUser.getId());

    // Generate JWT token
    String token = jwtService.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getRole().name());

    return new AuthDtos.AuthResponse(
        token,
        "Bearer",
        savedUser.getId(),
        savedUser.getUsername(),
        savedUser.getEmail(),
        savedUser.getRole().name(),
        savedUser.getStatus(),
        savedUser.isEmailVerified()
    );
  }

  /**
   * Authenticate user with username and password.
   *
   * @param req Login request containing username and password
   * @return AuthResponse with JWT access token
   * @throws InvalidCredentialsException if credentials are invalid
   * @throws UserNotFoundException if user account is inactive or suspended
   */
  @Transactional
  public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
    log.info("Login attempt for username: {}", req.username());

    try {
      // Authenticate using Spring Security
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(req.username(), req.password())
      );
      authentication.isAuthenticated();
    } catch (AuthenticationException e) {
      log.warn("Authentication failed for username: {}", req.username());
      throw new InvalidCredentialsException("Invalid username or password");
    }

    // Retrieve user from database
    User user = userRepository.findByUsername(req.username())
        .orElseThrow(() -> {
          log.error("User not found after authentication: {}", req.username());
          return new UserNotFoundException("User not found");
        });

    // Check account status
    if (user.getStatus() == UserStatus.SUSPENDED) {
      log.warn("Login attempt on suspended account: {}", req.username());
      throw new UserNotFoundException("Your account has been suspended. Please contact support.");
    }

    if (user.getStatus() == UserStatus.INACTIVE || user.getStatus() == UserStatus.DELETED) {
      log.warn("Login attempt on inactive/deleted account: {}", req.username());
      throw new UserNotFoundException("Your account is not active");
    }

    // Update last login timestamp
    user.setLastLogin(Instant.now());
    userRepository.save(user);

    // Generate JWT token
    String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().name());
    log.info("User logged in successfully: {}", req.username());

    return new AuthDtos.AuthResponse(
        token,
        "Bearer",
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getRole().name(),
        user.getStatus(),
        user.isEmailVerified()
    );
  }

  /**
   * Change user password with validation.
   *
   * @param userId User ID
   * @param req Change password request with current and new passwords
   * @throws UserNotFoundException if user not found
   * @throws InvalidCredentialsException if current password is incorrect
   */
  @Transactional
  public void changePassword(String userId, AuthDtos.ChangePasswordRequest req) {
    log.info("Password change request for user: {}", userId);

    // Validate new passwords match
    if (!req.newPassword().equals(req.confirmPassword())) {
      throw new InvalidCredentialsException("New passwords do not match");
    }

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    // Verify current password
    if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
      log.warn("Invalid current password for user: {}", userId);
      throw new InvalidCredentialsException("Current password is incorrect");
    }

    // Update password
    user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    userRepository.save(user);
    log.info("Password changed successfully for user: {}", userId);
  }

  /**
   * Get user profile information.
   *
   * @param userId User ID
   * @return User entity
   * @throws UserNotFoundException if user not found
   */
  public User getUserById(String userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found"));
  }

  /**
   * Validate token and return JWT claims.
   *
   * @param token JWT token
   * @return true if token is valid
   */
  public boolean validateToken(String token) {
    try {
      jwtService.parse(token);
      return true;
    } catch (Exception e) {
      log.warn("Token validation failed: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Register a new admin user (for testing/initial setup).
   * This method is intended for initial admin account creation.
   *
   * @param req Registration request containing username, email, password, displayName, userType
   * @return AuthResponse with JWT access token and ADMIN role
   * @throws UserAlreadyExistsException if username or email already exists
   */
  @Transactional
  public AuthDtos.AuthResponse registerAdmin(AuthDtos.RegisterRequest req) {
    log.info("Registering new admin user with username: {}", req.username());

    // Validate username uniqueness
    if (userRepository.existsByUsername(req.username())) {
      log.warn("Admin registration failed: Username already exists: {}", req.username());
      throw new UserAlreadyExistsException("Username '" + req.username() + "' is already taken");
    }

    // Validate email uniqueness
    if (userRepository.existsByEmail(req.email())) {
      log.warn("Admin registration failed: Email already exists: {}", req.email());
      throw new UserAlreadyExistsException("Email '" + req.email() + "' is already registered");
    }

    // Create new admin user
    User user = new User();
    user.setUsername(req.username());
    user.setEmail(req.email());
    user.setDisplayName(req.displayName());
    user.setUserType(req.userType());
    user.setPasswordHash(passwordEncoder.encode(req.password()));
    user.setRole(UserRole.ADMIN);
    user.setStatus(UserStatus.ACTIVE);
    user.setEmailVerified(false);

    User savedUser = userRepository.save(user);
    log.info("Admin user registered successfully with id: {}", savedUser.getId());

    // Generate JWT token
    String token = jwtService.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getRole().name());

    return new AuthDtos.AuthResponse(
        token,
        "Bearer",
        savedUser.getId(),
        savedUser.getUsername(),
        savedUser.getEmail(),
        savedUser.getRole().name(),
        savedUser.getStatus(),
        savedUser.isEmailVerified()
    );
  }
}
