package com.nabra.backend.modules.usermanagement.exception;

/**
 * Exception thrown when a user tries to register with an already existing username or email.
 */
public class UserAlreadyExistsException extends RuntimeException {
  public UserAlreadyExistsException(String message) {
    super(message);
  }

  public UserAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
