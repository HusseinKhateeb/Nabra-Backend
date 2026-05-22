package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.common.model.Enums.MessageType;
import com.nabra.backend.modules.chatcommunication.model.Chat;
import com.nabra.backend.modules.chatcommunication.model.Message;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatVoiceInferenceControllerTest {

    @Mock
    private MessageRepository messageRepository;

    private ChatVoiceInferenceController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatVoiceInferenceController(messageRepository);
        ReflectionTestUtils.setField(controller, "uploadAudioUrl", "http://localhost:8080/api/v1/lipreading/avsr/upload-audio");
    }

    @Test
    void inferVoiceMessageById_withBlankId_shouldReturnBadRequest() throws IOException {
        ResponseEntity<?> response = controller.inferVoiceMessageById("");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("voiceMessageId is required");
    }

    @Test
    void inferVoiceMessageById_withNullId_shouldReturnBadRequest() throws IOException {
        ResponseEntity<?> response = controller.inferVoiceMessageById("   ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("voiceMessageId is required");
    }

    @Test
    void inferVoiceMessageById_withNonExistentMessage_shouldReturnNotFound() throws IOException {
        when(messageRepository.findById("invalid")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.inferVoiceMessageById("invalid");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Voice message not found");
    }

    @Test
    void inferVoiceMessageById_withNonVoiceMessage_shouldReturnBadRequest() throws IOException {
        Message msg = new Message();
        msg.setType(MessageType.TEXT);
        msg.setId("m1");
        when(messageRepository.findById("m1")).thenReturn(Optional.of(msg));

        ResponseEntity<?> response = controller.inferVoiceMessageById("m1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Message is not a voice message");
    }

    @Test
    void inferVoiceMessageById_withNoAudioData_shouldReturnBadRequest() throws IOException {
        Message msg = new Message();
        msg.setType(MessageType.VOICE);
        msg.setId("m1");
        when(messageRepository.findById("m1")).thenReturn(Optional.of(msg));

        ResponseEntity<?> response = controller.inferVoiceMessageById("m1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("No audio file found for this message");
    }
}
