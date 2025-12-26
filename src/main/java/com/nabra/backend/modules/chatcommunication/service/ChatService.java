package com.nabra.backend.modules.chatcommunication.service;

import com.nabra.backend.modules.chatcommunication.dto.ChatDtos;
import com.nabra.backend.modules.chatcommunication.model.Chat;
import com.nabra.backend.modules.chatcommunication.repository.ChatRepository;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

  private final ChatRepository chatRepository;
  private final UserService userService;

  public ChatDtos.ChatResponse toDto(Chat c) {
    Set<String> pIds = c.getParticipants().stream().map(User::getId).collect(Collectors.toSet());
    return new ChatDtos.ChatResponse(c.getId(), c.isGroupChat(), c.getTitle(), pIds, c.getLastMessageAt());
  }

  public ChatDtos.ChatResponse create(String creatorUserId, ChatDtos.CreateChatRequest req) {
    Chat c = new Chat();
    c.setGroupChat(Boolean.TRUE.equals(req.groupChat()));
    c.setTitle(req.title());

    // Ensure creator is included.
    Set<String> ids = new java.util.HashSet<>(req.participantUserIds());
    ids.add(creatorUserId);

    Set<User> participants = ids.stream().map(userService::getById).collect(Collectors.toSet());
    c.setParticipants(participants);
    chatRepository.save(c);
    return toDto(c);
  }

  public Chat getChat(String chatId) {
    return chatRepository.findById(chatId).orElseThrow(() -> new IllegalArgumentException("Chat not found"));
  }

  public boolean isParticipant(Chat chat, String userId) {
    return chat.getParticipants().stream().anyMatch(u -> u.getId().equals(userId));
  }

  public Page<ChatDtos.ChatResponse> listForUser(String userId, Pageable pageable) {
    return chatRepository.findByParticipantsIdOrderByLastMessageAtDesc(userId, pageable)
        .map(this::toDto);
  }
}
