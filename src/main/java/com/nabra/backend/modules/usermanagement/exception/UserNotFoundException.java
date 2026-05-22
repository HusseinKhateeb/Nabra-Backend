package com.nabra.backend.modules.usermanagement.exception;

/**
 * Exception thrown when user account is not found or is inactive.
 */
public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(String message) {
    super(message);
  }

  public UserNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
