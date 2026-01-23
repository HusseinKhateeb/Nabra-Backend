package com.nabra.backend.common.web;

import com.nabra.backend.modules.usermanagement.exception.UserNotFoundException;
import com.nabra.backend.security.principal.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for security-related operations.
 * Provides helper methods to retrieve current authenticated user information.
 */
@Component
public class SecurityUtils {

  /**
   * Get the current authenticated user principal.
   *
   * @return UserPrincipal of the authenticated user
   * @throws UserNotFoundException if user is not authenticated
   */
  public static UserPrincipal currentPrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
      throw new UserNotFoundException("User is not authenticated");
    }
    return (UserPrincipal) auth.getPrincipal();
  }

  /**
   * Get the current authenticated user ID.
   *
   * @return User ID as String
   * @throws UserNotFoundException if user is not authenticated
   */
  public static String getCurrentUserId() {
    return currentPrincipal().getUserId();
  }

  /**
   * Get the current authenticated username.
   *
   * @return Username as String
   * @throws UserNotFoundException if user is not authenticated
   */
  public static String getCurrentUsername() {
    return currentPrincipal().getUsername();
  }

  /**
   * Get the current authenticated user entity.
   *
   * @return User entity
   * @throws UserNotFoundException if user is not authenticated
   */
  public static com.nabra.backend.modules.usermanagement.model.User getCurrentUser() {
    return currentPrincipal().getUser();
  }

  /**
   * Check if the current user is authenticated.
   *
   * @return true if user is authenticated, false otherwise
   */
  public static boolean isAuthenticated() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String);
  }

  /**
   * Check if the current user has a specific role.
   *
   * @param role Role to check (e.g., "ADMIN", "USER")
   * @return true if user has the role, false otherwise
   */
  public static boolean hasRole(String role) {
    if (!isAuthenticated()) {
      return false;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
  }
}
