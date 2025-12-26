package com.nabra.backend.modules.sessionhistory.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.sessionhistory.dto.SessionDtos;
import com.nabra.backend.modules.sessionhistory.service.SessionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions")
public class SessionController {

  private final SessionService sessionService;

  @PostMapping
  public ResponseEntity<SessionDtos.SessionResponse> start(@Valid @RequestBody SessionDtos.StartSessionRequest req) {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(sessionService.start(p.getUserId(), req));
  }

  @PostMapping("/{sessionId}/stop")
  public ResponseEntity<SessionDtos.SessionResponse> stop(
      @PathVariable String sessionId,
      @Valid @RequestBody SessionDtos.StopSessionRequest req
  ) {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(sessionService.stop(p.getUserId(), sessionId, req));
  }

  /**
   * Fetch session history with filters (date/type/keyword) as required by SRS.
   */
  @GetMapping
  public ResponseEntity<Page<SessionDtos.SessionResponse>> list(
      @Parameter(description = "Filter: from (inclusive) ISO instant")
      @RequestParam Optional<@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant> from,
      @Parameter(description = "Filter: to (inclusive) ISO instant")
      @RequestParam Optional<@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant> to,
      @Parameter(description = "Filter: outputType TEXT|VOICE")
      @RequestParam Optional<String> outputType,
      @Parameter(description = "Filter: keyword in result text")
      @RequestParam Optional<String> keyword,
      Pageable pageable
  ) {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(sessionService.list(p.getUserId(), from, to, outputType, keyword, pageable));
  }
}
