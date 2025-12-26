package com.nabra.backend.modules.admintools.dto;

import com.nabra.backend.common.model.Enums.ReportStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class ReportDtos {

  public record CreateReportRequest(
      @NotBlank String reportedUserId,
      String chatId,
      String messageId,
      @NotBlank String reason,
      String details
  ) {}

  public record ReportResponse(
      String id,
      String reporterId,
      String reportedUserId,
      String chatId,
      String messageId,
      String reason,
      String details,
      ReportStatus status,
      Instant reportedAt,
      String resolutionNote
  ) {}

  public record UpdateReportStatusRequest(
      ReportStatus status,
      String resolutionNote
  ) {}
}
