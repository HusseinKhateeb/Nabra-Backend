package com.nabra.backend.modules.admintools.controller;

import com.nabra.backend.modules.usermanagement.dto.UserDtos;
import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import com.nabra.backend.modules.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

  private final UserRepository userRepository;
  private final UserService userService;

  @GetMapping
  public ResponseEntity<Page<UserDtos.UserProfileResponse>> list(Pageable pageable) {
    return ResponseEntity.ok(userRepository.findAll(pageable).map(userService::toProfile));
  }
}
