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
   * Get a single session by ID. User can only view their own sessions.
   */
  @GetMapping("/{sessionId}")
  public ResponseEntity<SessionDtos.SessionResponse> getById(@PathVariable String sessionId) {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(sessionService.getById(p.getUserId(), sessionId));
  }

  /**
   * Fetch session history with comprehensive filters (date/type/status/duration/content/keyword).
   */
  @GetMapping
  public ResponseEntity<Page<SessionDtos.SessionResponse>> list(
      @Parameter(description = "Filter: from (inclusive) ISO instant")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> from,
      @Parameter(description = "Filter: to (inclusive) ISO instant")
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Optional<Instant> to,
      @Parameter(description = "Filter: sessionType LIP_READING|CHAT|VOICE_TO_TEXT|LEARNING")
      @RequestParam Optional<String> sessionType,
      @Parameter(description = "Filter: status ACTIVE|COMPLETED|FAILED")
      @RequestParam Optional<String> status,
      @Parameter(description = "Filter: outputType TEXT|VOICE")
      @RequestParam Optional<String> outputType,
      @Parameter(description = "Filter: keyword search in content and result text")
      @RequestParam Optional<String> keyword,
      @Parameter(description = "Filter: minimum duration in seconds")
      @RequestParam Optional<Long> minDuration,
      @Parameter(description = "Filter: maximum duration in seconds")
      @RequestParam Optional<Long> maxDuration,
      Pageable pageable
  ) {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(sessionService.list(
        p.getUserId(),
        from, to, sessionType, status, outputType, keyword, minDuration, maxDuration,
        pageable
    ));
  }

  /**
   * Delete a single session by ID. User can only delete their own sessions.
   */
  @DeleteMapping("/{sessionId}")
  public ResponseEntity<Void> delete(@PathVariable String sessionId) {
    var p = SecurityUtils.currentPrincipal();
    sessionService.deleteById(p.getUserId(), sessionId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Delete all sessions for the current user (privacy/data deletion).
   * Requires explicit confirmation via ?confirm=true query parameter.
   */
  @DeleteMapping
  public ResponseEntity<Void> deleteAll(
      @Parameter(description = "Confirmation flag to prevent accidental deletion")
      @RequestParam(required = false, defaultValue = "false") boolean confirm
  ) {
    if (!confirm) {
      return ResponseEntity.badRequest().build();
    }
    var p = SecurityUtils.currentPrincipal();
    sessionService.deleteAllUserSessions(p.getUserId());
    return ResponseEntity.noContent().build();
  }
}
