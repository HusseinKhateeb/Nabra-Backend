package com.nabra.backend.modules.chatcommunication.service;

import com.nabra.backend.common.model.Enums.DeliveryStatus;
import com.nabra.backend.modules.chatcommunication.dto.ChatDtos;
import com.nabra.backend.modules.chatcommunication.model.Chat;
import com.nabra.backend.modules.chatcommunication.model.Message;
import com.nabra.backend.modules.chatcommunication.repository.ChatRepository;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatRepository chatRepository;
  private final MessageRepository messageRepository;
  private final UserService userService;

  /* =======================
     Mapping
     ======================= */
  public ChatDtos.ChatResponse toDto(Chat c, String currentUserId) {

    Set<ChatDtos.ChatParticipantDto> participants =
        c.getParticipants().stream()
            .map(u -> new ChatDtos.ChatParticipantDto(
                u.getId(),
                u.getDisplayName(),
                u.getAvatarUrl()
            ))
            .collect(Collectors.toSet());

    Message lastMessage = messageRepository
        .findTopByChatIdOrderBySentAtDesc(c.getId())
        .orElse(null);

    String lastMessageText =
        lastMessage != null ? lastMessage.getTextContent() : "";

    long unreadCount = messageRepository.countUnreadMessages(
        c.getId(),
        currentUserId
    );

    return new ChatDtos.ChatResponse(
        c.getId(),
        c.isGroupChat(),
        c.getTitle(),
        participants,
        c.getLastMessageAt(),
        lastMessageText,
        unreadCount
    );
  }

  /* =======================
     Create chat
     ======================= */
  @Transactional
  public ChatDtos.ChatResponse create(
      String creatorUserId,
      ChatDtos.CreateChatRequest req
  ) {

    if (req.participantUserIds() == null || req.participantUserIds().isEmpty()) {
      throw new IllegalArgumentException("participantUserIds is required");
    }

    Set<String> ids = new HashSet<>(req.participantUserIds());
    ids.add(creatorUserId);

    if (!Boolean.TRUE.equals(req.groupChat()) && ids.size() < 2) {
      throw new IllegalArgumentException("Private chat must have 2 participants");
    }

    Chat chat = new Chat();
    chat.setGroupChat(Boolean.TRUE.equals(req.groupChat()));
    chat.setTitle(req.title());

    Set<User> participants = ids.stream()
        .map(userService::getById)
        .collect(Collectors.toSet());

    chat.setParticipants(participants);

    chatRepository.saveAndFlush(chat);

    return toDto(chat, creatorUserId);
  }

  /* =======================
     Get / List
     ======================= */
  public Chat getChat(String chatId) {
    return chatRepository.findById(chatId)
        .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
  }

  public boolean isParticipant(Chat chat, String userId) {
    return chat.getParticipants()
        .stream()
        .anyMatch(u -> u.getId().equals(userId));
  }

  @Transactional(readOnly = true)
  public boolean isParticipant(String chatId, String userId) {
    Chat chat = chatRepository.findById(chatId)
        .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
    return isParticipant(chat, userId);
  }

  @Transactional
  public Page<ChatDtos.ChatResponse> listForUser(
      String userId,
      Pageable pageable
  ) {
    return chatRepository
        .findByParticipantsIdOrderByLastMessageAtDesc(userId, pageable)
        .map(chat -> toDto(chat, userId));
  }

  @Transactional
  public void delete(String requesterUserId, String chatId) {
    Chat chat = getChat(chatId);

    if (!isParticipant(chat, requesterUserId)) {
      throw new IllegalArgumentException("Not a participant in this chat");
    }

    messageRepository.deleteAllByChatId(chatId);
    chatRepository.delete(chat);
  }

  // يُستخدم من MessageService
  public void save(Chat chat) {
    chatRepository.save(chat);
  }
}
