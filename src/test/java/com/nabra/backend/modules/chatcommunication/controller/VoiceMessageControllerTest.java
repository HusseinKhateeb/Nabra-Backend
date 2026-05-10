package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.common.model.Enums.DeliveryStatus;
import com.nabra.backend.common.model.Enums.MessageType;
import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.chatcommunication.dto.MessageDtos;
import com.nabra.backend.modules.chatcommunication.model.Chat;
import com.nabra.backend.modules.chatcommunication.model.Message;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import com.nabra.backend.modules.chatcommunication.service.ChatService;
import com.nabra.backend.modules.chatcommunication.service.MessageService;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.security.principal.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceMessageControllerTest {

    @Mock
    private MessageService messageService;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ChatService chatService;

    private VoiceMessageController controller;
    private MockedStatic<SecurityUtils> securityUtils;
    private UserPrincipal principal;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new VoiceMessageController(messageService, messageRepository, chatService);
        user = new User();
        user.setId("u1");
        user.setUsername("testuser");
        user.setDisplayName("Test");
        user.setEmail("test@test.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setUserType(UserType.OTHER);
        principal = new UserPrincipal(user);
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::currentPrincipal).thenReturn(principal);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void uploadVoiceMessage_withEmptyFile_shouldReturnBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("audioFile", "test.wav", "audio/wav", new byte[0]);

        ResponseEntity<?> response = controller.uploadVoiceMessage("c1", emptyFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("audioFile is required");
    }

    @Test
    void uploadVoiceMessage_withNullFile_shouldReturnBadRequest() throws Exception {
        ResponseEntity<?> response = controller.uploadVoiceMessage("c1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("audioFile is required");
    }

    @Test
    void uploadVoiceMessage_shouldReturnOk() throws Exception {
        Chat chat = new Chat();
        chat.setId("c1");

        Message savedMsg = new Message();
        savedMsg.setId("m1");
        savedMsg.setChat(chat);
        savedMsg.setSender(user);
        savedMsg.setType(MessageType.VOICE);
        savedMsg.setSentAt(Instant.now());

        when(chatService.getChat("c1")).thenReturn(chat);
        when(messageRepository.saveAndFlush(any())).thenReturn(savedMsg);

        var responseDto = new MessageDtos.MessageResponse("m1", "c1", "u1", MessageType.VOICE, null, null, null, DeliveryStatus.SENT, Instant.now());
        when(messageService.toDto(any())).thenReturn(responseDto);

        MockMultipartFile audioFile = new MockMultipartFile("audioFile", "test.wav", "audio/wav", new byte[]{1, 2, 3});

        ResponseEntity<?> response = controller.uploadVoiceMessage("c1", audioFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(responseDto);
        verify(messageService).updateChatLastMessageAt(eq(chat), any());
    }

    @Test
    void getVoiceMessageAudio_messageNotFound_shouldReturnNotFound() {
        when(messageRepository.findById("m1")).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = controller.getVoiceMessageAudio("c1", "m1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getVoiceMessageAudio_chatMismatch_shouldReturnNotFound() {
        Chat otherChat = new Chat();
        otherChat.setId("c2");
        Message msg = new Message();
        msg.setId("m1");
        msg.setChat(otherChat);
        when(messageRepository.findById("m1")).thenReturn(Optional.of(msg));

        ResponseEntity<byte[]> response = controller.getVoiceMessageAudio("c1", "m1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getVoiceMessageAudio_noAudioData_shouldReturnNoContent() {
        Chat chat = new Chat();
        chat.setId("c1");
        Message msg = new Message();
        msg.setId("m1");
        msg.setChat(chat);
        when(messageRepository.findById("m1")).thenReturn(Optional.of(msg));

        ResponseEntity<byte[]> response = controller.getVoiceMessageAudio("c1", "m1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void getVoiceMessageAudio_shouldReturnAudio() {
        Chat chat = new Chat();
        chat.setId("c1");
        Message msg = new Message();
        msg.setId("m1");
        msg.setChat(chat);
        msg.setAudioData(new byte[]{1, 2, 3, 4});
        when(messageRepository.findById("m1")).thenReturn(Optional.of(msg));

        ResponseEntity<byte[]> response = controller.getVoiceMessageAudio("c1", "m1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(1, 2, 3, 4);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo("audio/wav");
    }
}
