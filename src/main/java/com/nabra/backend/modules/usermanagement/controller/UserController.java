package com.nabra.backend.modules.usermanagement.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.usermanagement.dto.UserDtos;
import com.nabra.backend.modules.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public ResponseEntity<UserDtos.UserProfileResponse> me() {
    var principal = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(userService.toProfile(principal.getUser()));
  }

  @PutMapping("/me")
  public ResponseEntity<UserDtos.UserProfileResponse> updateMe(@Valid @RequestBody UserDtos.UpdateProfileRequest req) {
    var principal = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(userService.updateProfile(principal.getUserId(), req));
  }
}
