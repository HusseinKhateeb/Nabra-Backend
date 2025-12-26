package com.nabra.backend.modules.lipreading.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.lipreading.dto.LipReadingDtos;
import com.nabra.backend.modules.lipreading.service.LipReadingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lipreading")
@RequiredArgsConstructor
@Tag(name = "Lip Reading")
public class LipReadingController {

  private final LipReadingService lipReadingService;

  /**
   * Cloud inference endpoint (conditional feature in SRS). Offline inference is done on-device.
   */
  @PostMapping("/infer")
  public ResponseEntity<LipReadingDtos.LipReadingResponse> infer(@Valid @RequestBody LipReadingDtos.LipReadingRequest req) {
    SecurityUtils.currentPrincipal(); // ensure authenticated
    return ResponseEntity.ok(lipReadingService.infer(req));
  }
}
