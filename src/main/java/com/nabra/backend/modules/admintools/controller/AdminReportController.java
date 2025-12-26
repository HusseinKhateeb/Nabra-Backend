package com.nabra.backend.modules.admintools.controller;

import com.nabra.backend.common.model.Enums.ReportStatus;
import com.nabra.backend.modules.admintools.dto.ReportDtos;
import com.nabra.backend.modules.admintools.model.Report;
import com.nabra.backend.modules.admintools.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin - Reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

  private final ReportService reportService;

  private static ReportDtos.ReportResponse toDto(Report r) {
    return new ReportDtos.ReportResponse(
        r.getId(),
        r.getReporter().getId(),
        r.getReportedUser().getId(),
        r.getChat() == null ? null : r.getChat().getId(),
        r.getMessage() == null ? null : r.getMessage().getId(),
        r.getReason(),
        r.getDetails(),
        r.getStatus(),
        r.getReportedAt(),
        r.getResolutionNote()
    );
  }

  @GetMapping
  public ResponseEntity<Page<ReportDtos.ReportResponse>> list(
      @RequestParam(required = false) ReportStatus status,
      Pageable pageable
  ) {
    return ResponseEntity.ok(reportService.list(status, pageable).map(AdminReportController::toDto));
  }

  @PutMapping("/{reportId}")
  public ResponseEntity<ReportDtos.ReportResponse> update(
      @PathVariable String reportId,
      @Valid @RequestBody ReportDtos.UpdateReportStatusRequest req
  ) {
    Report updated = reportService.updateStatus(reportId, req.status(), req.resolutionNote());
    return ResponseEntity.ok(toDto(updated));
  }
}
