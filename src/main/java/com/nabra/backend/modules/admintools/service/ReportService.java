package com.nabra.backend.modules.admintools.service;

import com.nabra.backend.common.model.Enums.ReportStatus;
import com.nabra.backend.modules.admintools.model.Report;
import com.nabra.backend.modules.admintools.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

  private final ReportRepository reportRepository;

  public Page<Report> list(ReportStatus status, Pageable pageable) {
    if (status == null) {
      return reportRepository.findAll(pageable);
    }
    return reportRepository.findByStatusOrderByReportedAtDesc(status, pageable);
  }

  public Report updateStatus(String reportId, ReportStatus status, String resolutionNote) {
    Report r = reportRepository.findById(reportId).orElseThrow(() -> new IllegalArgumentException("Report not found"));
    if (status != null) r.setStatus(status);
    if (resolutionNote != null) r.setResolutionNote(resolutionNote);
    return reportRepository.save(r);
  }
}
