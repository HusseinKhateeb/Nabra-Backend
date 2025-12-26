package com.nabra.backend.modules.usermanagement.service;

import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.modules.usermanagement.dto.AuthDtos;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import com.nabra.backend.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req) {
    if (userRepository.existsByUsername(req.username())) {
      throw new IllegalArgumentException("Username already in use");
    }
    if (userRepository.existsByEmail(req.email())) {
      throw new IllegalArgumentException("Email already in use");
    }

    User u = new User();
    u.setUsername(req.username());
    u.setEmail(req.email());
    u.setPasswordHash(passwordEncoder.encode(req.password()));
    u.setDisplayName(req.displayName());
    u.setUserType(req.userType());
    if (req.preferredLanguage() != null && !req.preferredLanguage().isBlank()) {
      u.setPreferredLanguage(req.preferredLanguage());
    }
    u.setRole(UserRole.USER);

    userRepository.save(u);

    String token = jwtService.generateToken(u.getId(), u.getUsername(), u.getRole().name());
    return new AuthDtos.AuthResponse(token, "Bearer", u.getId(), u.getUsername(), u.getRole().name(), u.getPreferredLanguage());
  }

  public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(req.username(), req.password())
    );

    User u = userRepository.findByUsername(req.username())
        .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

    String token = jwtService.generateToken(u.getId(), u.getUsername(), u.getRole().name());
    return new AuthDtos.AuthResponse(token, "Bearer", u.getId(), u.getUsername(), u.getRole().name(), u.getPreferredLanguage());
  }
}
