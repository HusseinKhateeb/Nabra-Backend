package com.nabra.backend.modules.chatcommunication.controller;
import java.security.Principal;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.chatcommunication.dto.ChatDtos;
import com.nabra.backend.modules.chatcommunication.service.ChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
@Tag(name = "Chats")
public class ChatController {

  private final ChatService chatService;

  @PostMapping
  public ResponseEntity<ChatDtos.ChatResponse> create(
      @Valid @RequestBody ChatDtos.CreateChatRequest req
  ) {
    var p = SecurityUtils.currentPrincipal(); // ✅
    return ResponseEntity.ok(
        chatService.create(p.getUserId(), req)
    );
  }

  @GetMapping
  public ResponseEntity<Page<ChatDtos.ChatResponse>> list(
      Pageable pageable
  ) {
    var p = SecurityUtils.currentPrincipal(); // ✅
    return ResponseEntity.ok(
        chatService.listForUser(p.getUserId(), pageable)
    );
  }
}
