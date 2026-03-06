package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.common.model.Enums;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.chatcommunication.dto.MessageDtos;
import com.nabra.backend.modules.chatcommunication.model.Message;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import com.nabra.backend.modules.chatcommunication.service.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats/{chatId}/voice-message")
@RequiredArgsConstructor
public class VoiceMessageController {
    private final MessageService messageService;
    private final MessageRepository messageRepository;

    @Value("${app.voice-message.upload-dir:voice-uploads}")
    private String uploadDir;

    @PostMapping(consumes = "multipart/form-data")
    @Transactional
    public ResponseEntity<?> uploadVoiceMessage(
            @PathVariable("chatId") String chatId,
            @RequestParam("audioFile") MultipartFile audioFile
    ) throws IOException {
        if (audioFile == null || audioFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("audioFile is required");
        }
        // Ensure upload directory exists
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();
        // Save file with unique name
        String fileName = "voice-" + UUID.randomUUID() + ".wav";
        File dest = new File(dir, fileName);
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            StreamUtils.copy(audioFile.getInputStream(), fos);
        }
        // Create message entity as VOICE type, just save file path
        var p = SecurityUtils.currentPrincipal();
        MessageDtos.SendMessageRequest req = new MessageDtos.SendMessageRequest(
                Enums.MessageType.VOICE,
                null, // textContent
                dest.getAbsolutePath(), // mediaUrl
                null // voiceTranscript
        );
        // Save as normal message, do not transcribe
        MessageDtos.MessageResponse response = messageService.send(p.getUserId(), chatId, req, null);
        return ResponseEntity.ok(response);
    }
}
