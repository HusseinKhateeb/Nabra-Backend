package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.common.model.Enums;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.chatcommunication.dto.MessageDtos;
import com.nabra.backend.modules.chatcommunication.model.Message;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import com.nabra.backend.modules.chatcommunication.service.MessageService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/chats/{chatId}/voice-message")
@RequiredArgsConstructor
public class VoiceMessageController {
    private final MessageService messageService;
    private final MessageRepository messageRepository;
    private final com.nabra.backend.modules.chatcommunication.service.ChatService chatService;

    /**
     * Upload a new voice message and store its audio data directly in the database.
     */
    @PostMapping(consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<?> uploadVoiceMessage(
            @PathVariable("chatId") String chatId,
            @RequestParam("audioFile") MultipartFile audioFile
    ) throws IOException {
        if (audioFile == null || audioFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("audioFile is required");
        }
        // Read audio file bytes
        byte[] audioBytes = audioFile.getBytes();
        // Create message entity as VOICE type, store audio bytes
        var p = SecurityUtils.currentPrincipal();
        Message message = new Message();
        message.setType(Enums.MessageType.VOICE);
        message.setTextContent(null);
        message.setMediaUrl(null);
        message.setVoiceTranscript(null);
        message.setAudioData(audioBytes);
        // Set chat and sender
        message.setChat(chatService.getChat(chatId));
        message.setSender(p.getUser());
        // Save message
        Message saved = messageRepository.saveAndFlush(message);
        // Update chat last message time
        messageService.updateChatLastMessageAt(saved.getChat(), saved.getSentAt());
        // Return response DTO
        MessageDtos.MessageResponse response = messageService.toDto(saved);
        return ResponseEntity.ok(response);
    }

    /**
     * Stream the audio data for a given messageId (voice message) so clients can play it.
     */
    @GetMapping("/{messageId}/audio")
    @Transactional
    public ResponseEntity<byte[]> getVoiceMessageAudio(
            @PathVariable("chatId") String chatId,
            @PathVariable("messageId") String messageId
    ) {
        // Find the message and check it belongs to the chat
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null || message.getChat() == null || !message.getChat().getId().equals(chatId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (message.getAudioData() == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
        // Default to WAV, you can adjust if you support other formats
        return ResponseEntity.ok()
                .header("Content-Type", "audio/wav")
                .header("Content-Disposition", "inline; filename=voice-message.wav")
                .body(message.getAudioData());
    }
}
