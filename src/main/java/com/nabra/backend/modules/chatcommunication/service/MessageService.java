package com.nabra.backend.modules.chatcommunication.service;

import com.nabra.backend.common.model.Enums.MessageType;
import com.nabra.backend.modules.chatcommunication.dto.MessageDtos;
import com.nabra.backend.modules.chatcommunication.model.Message;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {

  private final MessageRepository messageRepository;
  private final ChatService chatService;
  private final UserService userService;
  private final VoiceToTextService voiceToTextService;

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

  public MessageDtos.MessageResponse send(String senderId, String chatId, MessageDtos.SendMessageRequest req, String preferredLanguage) {
    var chat = chatService.getChat(chatId);
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

    // Voice-to-text conversion (SRS FR-9): if client didn't provide transcript and STT is configured.
    if (req.type() == MessageType.VOICE) {
      String transcript = req.voiceTranscript();
      if ((transcript == null || transcript.isBlank()) && req.mediaUrl() != null && !req.mediaUrl().isBlank()) {
        transcript = voiceToTextService.transcribe(req.mediaUrl(), preferredLanguage);
      }
      m.setVoiceTranscript(transcript);
    } else {
      m.setVoiceTranscript(req.voiceTranscript());
    }

    messageRepository.save(m);
    chat.setLastMessageAt(m.getSentAt());
    // Chat saved by JPA dirty checking.

    return toDto(m);
  }

  public Page<MessageDtos.MessageResponse> list(String userId, String chatId, Pageable pageable) {
    var chat = chatService.getChat(chatId);
    if (!chatService.isParticipant(chat, userId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }
    return messageRepository.findByChatIdOrderBySentAtDesc(chatId, pageable).map(this::toDto);
  }
}
