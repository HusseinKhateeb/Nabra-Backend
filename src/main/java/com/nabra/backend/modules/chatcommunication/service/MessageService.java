package com.nabra.backend.modules.chatcommunication.service;

import com.nabra.backend.common.model.Enums.DeliveryStatus;
import com.nabra.backend.common.model.Enums.MessageType;
import com.nabra.backend.modules.chatcommunication.dto.MessageDtos;
import com.nabra.backend.modules.chatcommunication.model.Chat;
import com.nabra.backend.modules.chatcommunication.model.Message;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

  private final MessageRepository messageRepository;
  private final ChatService chatService;
  private final UserService userService;
  private final VoiceToTextService voiceToTextService;

  // ================= Mapping =================
  public MessageDtos.MessageResponse toDto(Message m) {
    return new MessageDtos.MessageResponse(
        m.getId(),
        m.getChat().getId(),
        m.getSender().getId(),
        m.getType(),
        m.getTextContent(),
        m.getMediaUrl(),
        m.getVoiceTranscript(),
        m.getDeliveryStatus(),
        m.getSentAt()
    );
  }

  // ================= Send =================
  @Transactional
  public MessageDtos.MessageResponse send(
      String senderId,
      String chatId,
      MessageDtos.SendMessageRequest req,
      String preferredLanguage
  ) {
    Chat chat = chatService.getChat(chatId);

    if (!chatService.isParticipant(chat, senderId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }

    User sender = userService.getById(senderId);

    Message m = new Message();
    m.setChat(chat);
    m.setSender(sender);
    m.setType(req.type());
    m.setTextContent(req.textContent());
    m.setMediaUrl(req.mediaUrl());
    m.setDeliveryStatus(DeliveryStatus.SENT);

    if (req.type() == MessageType.VOICE) {
      String transcript = req.voiceTranscript();
      if ((transcript == null || transcript.isBlank())
          && req.mediaUrl() != null
          && !req.mediaUrl().isBlank()) {
        transcript =
            voiceToTextService.transcribe(req.mediaUrl(), preferredLanguage);
      }
      m.setVoiceTranscript(transcript);
    } else {
      m.setVoiceTranscript(req.voiceTranscript());
    }

    Message saved = messageRepository.saveAndFlush(m);

    chat.setLastMessageAt(saved.getSentAt());
    chatService.save(chat);

    return toDto(saved);
  }

  // ================= List =================
  public Page<MessageDtos.MessageResponse> list(
      String userId,
      String chatId,
      Pageable pageable
  ) {
    Chat chat = chatService.getChat(chatId);

    if (!chatService.isParticipant(chat, userId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }

    return messageRepository
        .findByChatIdOrderBySentAtDesc(chatId, pageable)
        .map(this::toDto);
  }

  // ================= Delivered =================
  @Transactional
  public List<String> markDelivered(String userId, String chatId) {
    Chat chat = chatService.getChat(chatId);

    if (!chatService.isParticipant(chat, userId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }

    List<String> ids = messageRepository.findAllUnreadIds(chatId, userId);

    if (ids.isEmpty()) return Collections.emptyList();

    messageRepository.bulkUpdateStatus(ids, DeliveryStatus.DELIVERED);
    return ids;
  }

  // ================= Read (specific IDs) =================
  @Transactional
  public List<String> markRead(
      String userId,
      String chatId,
      List<String> messageIds
  ) {
    if (messageIds == null || messageIds.isEmpty()) {
      return Collections.emptyList();
    }

    Chat chat = chatService.getChat(chatId);

    if (!chatService.isParticipant(chat, userId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }

    messageRepository.bulkUpdateStatus(messageIds, DeliveryStatus.READ);
    return messageIds;
  }

  // ================= Read ALL (🔥 الحل الأساسي) =================
  @Transactional
  public void markAllAsRead(String userId, String chatId) {
    Chat chat = chatService.getChat(chatId);

    if (!chatService.isParticipant(chat, userId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }

    List<String> ids =
        messageRepository.findAllUnreadIds(chatId, userId);

    if (!ids.isEmpty()) {
      messageRepository.bulkUpdateStatus(ids, DeliveryStatus.READ);
    }
  }
}
