package com.nabra.backend.common.exception;

import com.nabra.backend.modules.usermanagement.exception.InvalidCredentialsException;
import com.nabra.backend.modules.usermanagement.exception.UserAlreadyExistsException;
import com.nabra.backend.modules.usermanagement.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;



import java.util.HashMap;
import java.util.Map;
import org.apache.catalina.connector.ClientAbortException;

/**
 * Global exception handler for REST API.
 * Handles all exceptions thrown during request processing and returns
 * standardized error responses in ApiError format.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Handle validation errors from @Valid annotations.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    log.warn("Validation error at {}: {}", req.getRequestURI(), ex.getBindingResult().getErrorCount());

    Map<String, Object> details = new HashMap<>();
    Map<String, String> fields = new HashMap<>();

    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fields.put(fe.getField(), fe.getDefaultMessage());
    }

    details.put("fields", fields);
    ApiError body = ApiError.of(400, "Bad Request", "Validation failed", req.getRequestURI(), details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  /**
   * Handle user already exists exception.
   * Returned when user tries to register with existing username or email.
   */
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ApiError> handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest req) {
    log.warn("User already exists error at {}: {}", req.getRequestURI(), ex.getMessage());
    ApiError body = ApiError.of(409, "Conflict", ex.getMessage(), req.getRequestURI(), Map.of());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  /**
   * Handle invalid credentials exception.
   * Returned when login credentials are incorrect or password change fails.
   */
  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
    log.warn("Invalid credentials error at {}", req.getRequestURI());
    ApiError body = ApiError.of(401, "Unauthorized", ex.getMessage(), req.getRequestURI(), Map.of());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
  }

  /**
   * Handle user not found exception.
   * Returned when user doesn't exist or account is inactive.
   */
  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ApiError> handleUserNotFound(UserNotFoundException ex, HttpServletRequest req) {
    log.warn("User not found error at {}: {}", req.getRequestURI(), ex.getMessage());
    ApiError body = ApiError.of(404, "Not Found", ex.getMessage(), req.getRequestURI(), Map.of());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  /**
   * Handle Spring Security authentication exceptions.
   */
  @ExceptionHandler({BadCredentialsException.class, InternalAuthenticationServiceException.class})
  public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest req) {
    log.warn("Authentication error at {}: {}", req.getRequestURI(), ex.getMessage());
    ApiError body = ApiError.of(401, "Unauthorized", "Authentication failed. Invalid credentials.", req.getRequestURI(), Map.of());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
  }

  /**
   * Handle illegal argument exceptions.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
    log.warn("Illegal argument error at {}: {}", req.getRequestURI(), ex.getMessage());
    ApiError body = ApiError.of(400, "Bad Request", ex.getMessage(), req.getRequestURI(), Map.of());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  /**
   * Handle all other exceptions.
   * Suppress stack trace for ClientAbortException wrapped in generic Exception.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
    Throwable cause = ex.getCause();
    if (ex instanceof ClientAbortException || (cause != null && cause instanceof ClientAbortException)) {
      log.debug("Client aborted connection at {}: {}", req.getRequestURI(), ex.getMessage());
      return null; // No response, let Tomcat handle
    }
    log.error("Unexpected error at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
    ApiError body = ApiError.of(500, "Internal Server Error", "An unexpected error occurred", req.getRequestURI(), Map.of());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  /**
   * Suppress logging for ClientAbortException (e.g., client cancels video download).
   */
  @ExceptionHandler(ClientAbortException.class)
  public void handleClientAbort(ClientAbortException ex, HttpServletRequest req) {
    // Optionally log at debug level, but do not return a response or log as error
    log.debug("Client aborted connection at {}: {}", req.getRequestURI(), ex.getMessage());
    // No response body, just let Spring/Tomcat handle the disconnect
  }
}
