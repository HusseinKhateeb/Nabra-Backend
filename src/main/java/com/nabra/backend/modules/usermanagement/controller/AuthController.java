package com.nabra.backend.modules.usermanagement.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.usermanagement.dto.AuthDtos;
import com.nabra.backend.modules.usermanagement.dto.UserDtos;
import com.nabra.backend.modules.usermanagement.service.AuthService;
import com.nabra.backend.modules.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Handles user registration, login, and password management.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user authentication and authorization")
public class AuthController {

  private final AuthService authService;
  private final UserService userService;
  private final SecurityUtils securityUtils;

  /**
   * Register a new user account.
   *
   * @param req Registration request with username, email, password, displayName, userType
   * @return AuthResponse with JWT access token
   */
  @PostMapping("/register")
  @Operation(summary = "Register a new user", description = "Create a new user account with username, email, and password")
  public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
    log.info("Register endpoint called for username: {}", req.username());
    AuthDtos.AuthResponse response = authService.register(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Register a new admin account (for testing/initial setup).
   *
   * @param req Registration request with username, email, password, displayName, userType
   * @return AuthResponse with JWT access token and ADMIN role
   */
  @PostMapping("/register-admin")
  @Operation(summary = "Register a new admin user", description = "Create a new admin account (for testing/initial setup)")
  public ResponseEntity<AuthDtos.AuthResponse> registerAdmin(@Valid @RequestBody AuthDtos.RegisterRequest req) {
    log.info("Register admin endpoint called for username: {}", req.username());
    AuthDtos.AuthResponse response = authService.registerAdmin(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Authenticate user and return JWT token.
   *
   * @param req Login request with username and password
   * @return AuthResponse with JWT access token
   */
  @PostMapping("/login")
  @Operation(summary = "Login user", description = "Authenticate user with username and password, returns JWT token")
  public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
    log.info("Login endpoint called for username: {}", req.username());
    AuthDtos.AuthResponse response = authService.login(req);
    return ResponseEntity.ok(response);
  }

  /**
   * Change password for authenticated user.
   * Requires valid JWT token.
   *
   * @param req Change password request with current and new passwords
   * @return Response entity with success message
   */
  @PostMapping("/change-password")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Change password", description = "Change password for authenticated user")
  public ResponseEntity<String> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest req) {
    String userId = securityUtils.getCurrentUserId();
    log.info("Change password request for user: {}", userId);
    authService.changePassword(userId, req);
    return ResponseEntity.ok("Password changed successfully");
  }

  /**
   * Validate JWT token.
   *
   * @param token JWT token to validate
   * @return ResponseEntity with validation result
   */
  @GetMapping("/validate")
  @Operation(summary = "Validate token", description = "Validate if JWT token is still valid")
  public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
    boolean isValid = authService.validateToken(token);
    return ResponseEntity.ok(isValid);
  }

  /**
   * Get current authenticated user's information.
   * Requires valid JWT token.
   *
   * @return UserProfileResponse with user details
   */
  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Get current user", description = "Get authenticated user's profile information")
  public ResponseEntity<Object> getCurrentUser() {
    String userId = securityUtils.getCurrentUserId();
    log.info("Get current user request for user: {}", userId);
    var user = authService.getUserById(userId);
    return ResponseEntity.ok(user);
  }

  /**
   * Update user profile information.
   * Requires valid JWT token.
   *
   * @param req Profile update request
   * @return Updated UserProfileResponse
   */
  @PostMapping("/update-profile")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Update user profile", description = "Update authenticated user's profile information")
  public ResponseEntity<UserDtos.UserProfileResponse> updateProfile(@Valid @RequestBody UserDtos.UpdateProfileRequest req) {
    String userId = securityUtils.getCurrentUserId();
    log.info("Update profile request for user: {}", userId);
    UserDtos.UserProfileResponse response = userService.updateProfile(userId, req);
    return ResponseEntity.ok(response);
  }

  /**
   * Logout user (invalidate token on client side).
   * In a stateless JWT-based system, logout is handled client-side.
   * This endpoint can be used for logging/audit purposes.
   *
   * @return Response entity with success message
   */
  @PostMapping("/logout")
  @PreAuthorize("isAuthenticated()")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(summary = "Logout user", description = "Logout user (client should discard JWT token)")
  public ResponseEntity<String> logout() {
    String userId = securityUtils.getCurrentUserId();
    log.info("Logout request for user: {}", userId);
    return ResponseEntity.ok("Logged out successfully. Please discard your token.");
  }
}
