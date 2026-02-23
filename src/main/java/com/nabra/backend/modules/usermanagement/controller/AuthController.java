package com.nabra.backend.modules.usermanagement.controller;

import com.nabra.backend.modules.usermanagement.dto.AuthDtos;
import com.nabra.backend.modules.usermanagement.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
    return ResponseEntity.ok(authService.register(req));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
    return ResponseEntity.ok(authService.login(req));
  }
}
