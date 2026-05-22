package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.chatcommunication.model.Message;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatVoiceInferenceController {
    private static final Logger log = LoggerFactory.getLogger(ChatVoiceInferenceController.class);
    private final MessageRepository messageRepository;

    @Value("${lipreading.upload-audio.url:http://localhost:8080/api/v1/lipreading/avsr/upload-audio}")
    private String uploadAudioUrl;

    @PostMapping("/voice-infer")
    @Transactional
    public ResponseEntity<?> inferVoiceMessageById(
            @RequestParam("voiceMessageId") String voiceMessageId) throws IOException {
        if (voiceMessageId == null || voiceMessageId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("voiceMessageId is required");
        }
        Optional<Message> messageOpt = messageRepository.findById(voiceMessageId);
        if (messageOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Voice message not found");
        }
        Message message = messageOpt.get();
        if (message.getType() != com.nabra.backend.common.model.Enums.MessageType.VOICE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Message is not a voice message");
        }
        File audioFile = null;
        String mediaUrl = message.getMediaUrl();
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            audioFile = new File(mediaUrl);
            if (!audioFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Audio file not found on server");
            }
        } else if (message.getAudioData() != null && message.getAudioData().length > 0) {
            // Write BLOB to temp file
            audioFile = File.createTempFile("voice-msg-", ".wav");
            try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                fos.write(message.getAudioData());
            }
            audioFile.deleteOnExit();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No audio file found for this message");
        }
        RestTemplate restTemplate = new RestTemplate();
        FileSystemResource audioResource = new FileSystemResource(audioFile);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        // Copy Authorization header from incoming request
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String auth = attrs.getRequest().getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
        }
        MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("audioFile", audioResource);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(uploadAudioUrl, requestEntity, String.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}
