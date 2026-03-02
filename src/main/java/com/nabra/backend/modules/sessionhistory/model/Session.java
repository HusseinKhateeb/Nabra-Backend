package com.nabra.backend.modules.sessionhistory.model;

import com.nabra.backend.common.model.BaseEntity;
import com.nabra.backend.common.model.Enums.SessionInputType;
import com.nabra.backend.common.model.Enums.SessionOutputType;
import com.nabra.backend.common.model.Enums.SessionStatus;
import com.nabra.backend.common.model.Enums.SessionType;
import com.nabra.backend.modules.usermanagement.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sessions", indexes = {
    @Index(name = "idx_sessions_user_started", columnList = "user_id, startedAt"),
    @Index(name = "idx_sessions_output_type", columnList = "outputType"),
    @Index(name = "idx_sessions_type", columnList = "sessionType"),
    @Index(name = "idx_sessions_status", columnList = "status")
})
@Getter
@Setter
public class Session extends BaseEntity {

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** Session type: LIP_READING, CHAT, VOICE_TO_TEXT, or LEARNING. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionType sessionType = SessionType.LIP_READING;

  /** Live camera or recorded video input. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionInputType inputType = SessionInputType.LIVE;

  /** Output choice: Arabic text or TTS audio. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionOutputType outputType = SessionOutputType.TEXT;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SessionStatus status = SessionStatus.ACTIVE;

  @Column(nullable = false)
  private Instant startedAt = Instant.now();

  private Instant endedAt;

  /** Derived when session ends. */
  private Long durationSeconds;

  /** Main content: transcript, chat summary, recognized text, learning progress summary. */
  @Column(length = 5000)
  private String content;

  /** Optional reference to related record (messageId, chatId, learningProgressId). */
  @Column(length = 100)
  private String contentRefId;

  /** Final recognized Arabic text result (single sentence as per SRS scope). */
  @Column(length = 2000)
  private String resultText;

  /** Optional URL/path to synthesized audio file (if outputType=VOICE). */
  @Column(length = 400)
  private String resultAudioUrl;

  /** Optional accuracy metric (e.g., word error rate or confidence). */
  private Double accuracyScore;

  /** Device information (browser, OS, device type). */
  @Column(length = 500)
  private String deviceInfo;

  /** Model/algorithm version used in this session. */
  @Column(length = 50)
  private String modelVersion;

  /** Whether session was processed offline. */
  private Boolean isOffline = false;
}
