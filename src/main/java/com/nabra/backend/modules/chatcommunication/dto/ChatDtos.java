package com.nabra.backend.modules.chatcommunication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

public class ChatDtos {

  public record CreateChatRequest(
      @NotNull Boolean groupChat,
      String title,
      @NotEmpty Set<String> participantUserIds
  ) {}

  public record ChatResponse(
      String id,
      boolean groupChat,
      String title,
      Set<String> participantUserIds,
      Instant lastMessageAt
  ) {}
}
