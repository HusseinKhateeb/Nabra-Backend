package com.nabra.backend.modules.admintools.repository;

import com.nabra.backend.modules.admintools.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {
  Page<Report> findByStatusOrderByReportedAtDesc(com.nabra.backend.common.model.Enums.ReportStatus status, Pageable pageable);
}
