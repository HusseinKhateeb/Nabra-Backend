package com.nabra.backend.modules.sessionhistory.dto;

import com.nabra.backend.common.model.Enums.SessionInputType;
import com.nabra.backend.common.model.Enums.SessionOutputType;
import com.nabra.backend.common.model.Enums.SessionStatus;
import com.nabra.backend.common.model.Enums.SessionType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class SessionDtos {

  public record StartSessionRequest(
      @NotNull SessionType sessionType,
      @NotNull SessionInputType inputType,
      @NotNull SessionOutputType outputType,
      String title,
      String contentPreview,
      String deviceInfo,
      String modelVersion,
      Boolean isOffline
  ) {}

  public record StopSessionRequest(
      String content,
      String contentRefId,
      String resultText,
      String resultAudioUrl,
      Double accuracyScore
  ) {}

  public record SessionResponse(
      String id,
      String userId,
      SessionType sessionType,
      SessionInputType inputType,
      SessionOutputType outputType,
      SessionStatus status,
      Instant startedAt,
      Instant endedAt,
      Long durationSeconds,
      String content,
      String contentRefId,
      String resultText,
      String resultAudioUrl,
      Double accuracyScore,
      String deviceInfo,
      String modelVersion,
      Boolean isOffline
  ) {}
}
