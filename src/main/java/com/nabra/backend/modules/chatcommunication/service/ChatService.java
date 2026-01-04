package com.nabra.backend.modules.chatcommunication.service;

import com.nabra.backend.modules.chatcommunication.dto.ChatDtos;
import com.nabra.backend.modules.chatcommunication.model.Chat;
import com.nabra.backend.modules.chatcommunication.repository.ChatRepository;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.service.UserService;
import jakarta.transaction.Transactional;
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
  private final UserService userService;

  public ChatDtos.ChatResponse toDto(Chat c) {
    Set<String> pIds =
        c.getParticipants().stream().map(User::getId).collect(Collectors.toSet());

    return new ChatDtos.ChatResponse(
        c.getId(),
        c.isGroupChat(),
        c.getTitle(),
        pIds,
        c.getLastMessageAt()
    );
  }

  // ✅ التعديل الأهم هنا
  @Transactional
  public ChatDtos.ChatResponse create(String creatorUserId, ChatDtos.CreateChatRequest req) {

    if (req.participantUserIds() == null || req.participantUserIds().isEmpty()) {
      throw new IllegalArgumentException("participantUserIds is required");
    }

    Set<String> ids = new HashSet<>(req.participantUserIds());
    ids.add(creatorUserId);

    // ❌ منع محادثة مع النفس
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

    // ✅ مهم جدًا: saveAndFlush
    chatRepository.saveAndFlush(chat);

    return toDto(chat);
  }

  public Chat getChat(String chatId) {
    return chatRepository.findById(chatId)
        .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
  }

  public boolean isParticipant(Chat chat, String userId) {
    return chat.getParticipants()
        .stream()
        .anyMatch(u -> u.getId().equals(userId));
  }

  public Page<ChatDtos.ChatResponse> listForUser(String userId, Pageable pageable) {
    return chatRepository
        .findByParticipantsIdOrderByLastMessageAtDesc(userId, pageable)
        .map(this::toDto);
  }

  // يُستخدم في MessageService
  public void save(Chat chat) {
    chatRepository.save(chat);
  }
}
