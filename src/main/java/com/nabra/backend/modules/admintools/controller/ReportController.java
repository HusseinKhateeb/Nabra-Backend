package com.nabra.backend.modules.admintools.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.admintools.dto.ReportDtos;
import com.nabra.backend.modules.admintools.model.Report;
import com.nabra.backend.modules.admintools.repository.ReportRepository;
import com.nabra.backend.modules.chatcommunication.repository.ChatRepository;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import com.nabra.backend.modules.usermanagement.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportController {

  private final ReportRepository reportRepository;
  private final UserService userService;
  private final ChatRepository chatRepository;
  private final MessageRepository messageRepository;

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

  @PostMapping
  public ResponseEntity<ReportDtos.ReportResponse> create(@Valid @RequestBody ReportDtos.CreateReportRequest req) {
    var p = SecurityUtils.currentPrincipal();
    Report r = new Report();
    r.setReporter(userService.getById(p.getUserId()));
    r.setReportedUser(userService.getById(req.reportedUserId()));
    if (req.chatId() != null) r.setChat(chatRepository.findById(req.chatId()).orElse(null));
    if (req.messageId() != null) r.setMessage(messageRepository.findById(req.messageId()).orElse(null));
    r.setReason(req.reason());
    r.setDetails(req.details());
    reportRepository.save(r);
    return ResponseEntity.ok(toDto(r));
  }
}
