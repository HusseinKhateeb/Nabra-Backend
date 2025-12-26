package com.nabra.backend.modules.chatcommunication.model;

import com.nabra.backend.common.model.BaseEntity;
import com.nabra.backend.common.model.Enums.DeliveryStatus;
import com.nabra.backend.common.model.Enums.MessageType;
import com.nabra.backend.modules.usermanagement.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_messages_chat_sentAt", columnList = "chat_id, sentAt"),
    @Index(name = "idx_messages_sender", columnList = "sender_id")
})
@Getter
@Setter
public class Message extends BaseEntity {

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "chat_id", nullable = false)
  private Chat chat;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MessageType type = MessageType.TEXT;

  /** Message text content (Arabic/English). */
  @Column(length = 4000)
  private String textContent;

  /** For images/files/voice: a URL in S3 or similar. */
  @Column(length = 600)
  private String mediaUrl;

  /** For voice messages: Speech-to-text output in Arabic. */
  @Column(length = 4000)
  private String voiceTranscript;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

  @Column(nullable = false)
  private Instant sentAt = Instant.now();
}
