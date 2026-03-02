package com.nabra.backend.modules.sessionhistory.dto;

import com.nabra.backend.common.model.Enums.SessionInputType;
import com.nabra.backend.common.model.Enums.SessionOutputType;
import com.nabra.backend.common.model.Enums.SessionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class SessionDtos {

  /** Start a lip reading session (sessionType is always LIP_READING). */
  public record StartSessionRequest(
      @NotNull SessionInputType inputType,
      @NotNull SessionOutputType outputType,
      String deviceInfo,
      String modelVersion,
      Boolean isOffline
  ) {}

  public record StopSessionRequest(
      String content,
      String resultText,
      String resultAudioUrl,
      Double accuracyScore
  ) {}

  /** Response DTO for lip reading session. */
  public record SessionResponse(
      String id,
      String userId,
      SessionInputType inputType,
      SessionOutputType outputType,
      SessionStatus status,
      Instant startedAt,
      Instant endedAt,
      Long durationSeconds,
      String resultText,
      String resultAudioUrl,
      Double accuracyScore,
      String deviceInfo,
      String modelVersion,
      Boolean isOffline
  ) {}
}
