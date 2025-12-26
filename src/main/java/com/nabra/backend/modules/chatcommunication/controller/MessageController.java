package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.chatcommunication.dto.MessageDtos;
import com.nabra.backend.modules.chatcommunication.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chats/{chatId}/messages")
@RequiredArgsConstructor
@Tag(name = "Messages")
public class MessageController {

  private final MessageService messageService;

  @PostMapping
  public ResponseEntity<MessageDtos.MessageResponse> send(
      @PathVariable String chatId,
      @Valid @RequestBody MessageDtos.SendMessageRequest req
  ) {
    var p = SecurityUtils.currentPrincipal();
    String preferredLanguage = p.getUser().getPreferredLanguage();
    return ResponseEntity.ok(messageService.send(p.getUserId(), chatId, req, preferredLanguage));
  }

  @GetMapping
  public ResponseEntity<Page<MessageDtos.MessageResponse>> list(
      @PathVariable String chatId,
      Pageable pageable
  ) {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(messageService.list(p.getUserId(), chatId, pageable));
  }
}
