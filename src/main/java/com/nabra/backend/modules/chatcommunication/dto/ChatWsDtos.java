package com.nabra.backend.modules.chatcommunication.dto;

import com.nabra.backend.common.model.Enums.MessageType;
import java.time.Instant;
import java.util.List;

public class ChatWsDtos {

  // العميل يبعت رسالة
  public record WsSendMessage(
      String chatId,
      MessageType type,
      String textContent,
      String mediaUrl,
      String voiceTranscript
  ) {}

  // حدث رسالة راجعة للكل
public record WsMessageEvent(
    String eventType,
    Object message
) {}


  // typing
  public record WsTyping(
      String chatId,
      boolean typing
  ) {}

  public record WsTypingEvent(
      String eventType, // "TYPING"
      String chatId,
      String fromUserId,
      boolean typing,
      Instant at
  ) {}

  // seen/read receipt
  public record WsSeen(
      String chatId,
      List<String> messageIds // الرسائل اللي صارت READ
  ) {}

  public record WsSeenEvent(
      String eventType, // "SEEN"
      String chatId,
      String readerUserId,
      List<String> messageIds,
      Instant at
  ) {}

  // delivered (اختياري لكن مفيد)
  public record WsDeliveredEvent(
      String eventType, // "DELIVERED"
      String chatId,
      String receiverUserId,
      List<String> messageIds,
      Instant at
  ) {}
}
