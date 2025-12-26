package com.nabra.backend.modules.sessionhistory.dto;

import com.nabra.backend.common.model.Enums.SessionInputType;
import com.nabra.backend.common.model.Enums.SessionOutputType;
import com.nabra.backend.common.model.Enums.SessionStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class SessionDtos {

  public record StartSessionRequest(
      @NotNull SessionInputType inputType,
      @NotNull SessionOutputType outputType
  ) {}

  public record StopSessionRequest(
      String resultText,
      String resultAudioUrl,
      Double accuracyScore
  ) {}

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
      Double accuracyScore
  ) {}
}
