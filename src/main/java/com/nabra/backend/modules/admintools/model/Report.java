package com.nabra.backend.modules.admintools.model;

import com.nabra.backend.common.model.BaseEntity;
import com.nabra.backend.common.model.Enums.ReportStatus;
import com.nabra.backend.modules.chatcommunication.model.Chat;
import com.nabra.backend.modules.chatcommunication.model.Message;
import com.nabra.backend.modules.usermanagement.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_reports_status", columnList = "status")
})
@Getter
@Setter
public class Report extends BaseEntity {

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_user_id", nullable = false)
  private User reportedUser;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_id")
  private Chat chat;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "message_id")
  private Message message;

  @Column(nullable = false, length = 200)
  private String reason;

  @Column(length = 2000)
  private String details;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReportStatus status = ReportStatus.OPEN;

  @Column(nullable = false)
  private Instant reportedAt = Instant.now();

  /** Admin note. */
  @Column(length = 2000)
  private String resolutionNote;
}
