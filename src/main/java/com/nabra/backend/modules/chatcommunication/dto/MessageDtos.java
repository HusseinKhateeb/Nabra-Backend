package com.nabra.backend.modules.chatcommunication.dto;

import com.nabra.backend.common.model.Enums.DeliveryStatus;
import com.nabra.backend.common.model.Enums.MessageType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class MessageDtos {

  public record SendMessageRequest(
      @NotNull MessageType type,
      String textContent,
      String mediaUrl,
      String voiceTranscript
  ) {}

  public record MessageResponse(
      String id,
      String chatId,
      String senderId,
      MessageType type,
      String textContent,
      String mediaUrl,
      String voiceTranscript,
      DeliveryStatus deliveryStatus,
      Instant sentAt
  ) {}
}
