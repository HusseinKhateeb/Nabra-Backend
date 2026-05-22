package com.nabra.backend.modules.admintools.controller;

import com.nabra.backend.common.model.Enums.ReportStatus;
import com.nabra.backend.modules.admintools.dto.ReportDtos;
import com.nabra.backend.modules.admintools.model.Report;
import com.nabra.backend.modules.admintools.service.ReportService;
import com.nabra.backend.modules.usermanagement.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

    @Mock
    private ReportService reportService;

    private AdminReportController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminReportController(reportService);
    }

    @Test
    void list_withoutStatus_shouldReturnAll() {
        Report r = createReport("r1");
        Page<Report> page = new PageImpl<>(List.of(r));
        when(reportService.list(eq(null), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<ReportDtos.ReportResponse>> response = controller.list(null, PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).id()).isEqualTo("r1");
    }

    @Test
    void list_withStatus_shouldReturnFiltered() {
        Report r = createReport("r2");
        r.setStatus(ReportStatus.OPEN);
        Page<Report> page = new PageImpl<>(List.of(r));
        when(reportService.list(eq(ReportStatus.OPEN), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<ReportDtos.ReportResponse>> response = controller.list(ReportStatus.OPEN, PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void update_shouldReturnUpdatedReport() {
        Report r = createReport("r1");
        r.setStatus(ReportStatus.RESOLVED);
        r.setResolutionNote("Resolved");
        when(reportService.updateStatus(eq("r1"), eq(ReportStatus.RESOLVED), eq("Resolved"))).thenReturn(r);

        var req = new ReportDtos.UpdateReportStatusRequest(ReportStatus.RESOLVED, "Resolved");
        ResponseEntity<ReportDtos.ReportResponse> response = controller.update("r1", req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(ReportStatus.RESOLVED);
    }

    private Report createReport(String id) {
        Report r = new Report();
        r.setId(id);
        User reporter = new User();
        reporter.setId("reporter1");
        r.setReporter(reporter);
        User reported = new User();
        reported.setId("reported1");
        r.setReportedUser(reported);
        r.setReason("Test reason");
        r.setStatus(ReportStatus.OPEN);
        r.setReportedAt(Instant.now());
        return r;
    }
}
