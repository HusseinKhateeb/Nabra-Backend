package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.modules.chatcommunication.dto.ChatWsDtos;
import com.nabra.backend.modules.chatcommunication.service.ChatService;
import com.nabra.backend.modules.chatcommunication.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

  private final SimpMessagingTemplate messagingTemplate;
  private final ChatService chatService;
  private final MessageService messageService;

  /* =========================
     JOIN (DELIVERED)
     ========================= */
  @MessageMapping("/chat.join")
  public void join(@Payload String chatId, Principal principal) {
    String userId = principal.getName();

    var chat = chatService.getChat(chatId);
    if (!chatService.isParticipant(chat, userId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }

    List<String> deliveredIds = messageService.markDelivered(userId, chatId);
    if (!deliveredIds.isEmpty()) {
      messagingTemplate.convertAndSend(
          "/topic/chats/" + chatId,
          new ChatWsDtos.WsDeliveredEvent(
              "DELIVERED",
              chatId,
              userId,
              deliveredIds,
              Instant.now()
          )
      );
    }
  }

  /* =========================
     SEND (WS = BROADCAST ONLY)
     ========================= */
  @MessageMapping("/chat.send")
  public void send(@Payload ChatWsDtos.WsSendMessage req, Principal principal) {
    String userId = principal.getName();

    // ✅ تحقق من الصلاحية فقط
    var chat = chatService.getChat(req.chatId());
    if (!chatService.isParticipant(chat, userId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }

    // ❌ ممنوع الحفظ هنا (الحفظ يتم عبر REST فقط)
    // ❌ لا messageService.send()

    // ✅ بث الحدث فقط
    messagingTemplate.convertAndSend(
        "/topic/chats/" + req.chatId(),
        new ChatWsDtos.WsMessageEvent(
            "MESSAGE",
            req // فقط البيانات الخام
        )
    );
  }

  /* =========================
     TYPING
     ========================= */
  @MessageMapping("/chat.typing")
  public void typing(@Payload ChatWsDtos.WsTyping req, Principal principal) {
    String userId = principal.getName();

    var chat = chatService.getChat(req.chatId());
    if (!chatService.isParticipant(chat, userId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }

    messagingTemplate.convertAndSend(
        "/topic/chats/" + req.chatId(),
        new ChatWsDtos.WsTypingEvent(
            "TYPING",
            req.chatId(),
            userId,
            req.typing(),
            Instant.now()
        )
    );
  }

  /* =========================
     SEEN / READ
     ========================= */
  @MessageMapping("/chat.seen")
  public void seen(@Payload ChatWsDtos.WsSeen req, Principal principal) {
    String userId = principal.getName();

    List<String> readIds =
        messageService.markRead(userId, req.chatId(), req.messageIds());

    if (readIds.isEmpty()) return;

    messagingTemplate.convertAndSend(
        "/topic/chats/" + req.chatId(),
        new ChatWsDtos.WsSeenEvent(
            "SEEN",
            req.chatId(),
            userId,
            readIds,
            Instant.now()
        )
    );
  }
}
