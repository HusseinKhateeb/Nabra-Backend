package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.common.model.Enums.DeliveryStatus;
import com.nabra.backend.common.model.Enums.MessageType;
import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.chatcommunication.dto.MessageDtos;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    private MessageController controller;
    private MockedStatic<SecurityUtils> securityUtils;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new MessageController(messageService);
        User user = new User();
        user.setId("u1");
        user.setPreferredLanguage("ar");
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
    void send_shouldReturnOk() {
        var req = new MessageDtos.SendMessageRequest(MessageType.TEXT, "hello", null, null);
        var expected = new MessageDtos.MessageResponse("m1", "c1", "u1", MessageType.TEXT, "hello", null, null, DeliveryStatus.SENT, Instant.now());
        when(messageService.send(eq("u1"), eq("c1"), any(), eq("ar"))).thenReturn(expected);

        ResponseEntity<MessageDtos.MessageResponse> response = controller.send("c1", req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void transcribeMessage_shouldReturnResult() {
        var expected = new MessageDtos.MessageResponse("m1", "c1", "u1", MessageType.TEXT, "transcribed", null, null, DeliveryStatus.SENT, Instant.now());
        when(messageService.transcribeMessage("u1", "c1", "m1")).thenReturn(expected);

        ResponseEntity<?> response = controller.transcribeMessage("c1", "m1");

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void deleteMessage_shouldReturnNoContent() {
        ResponseEntity<?> response = controller.deleteMessage("c1", "m1");

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
        verify(messageService).deleteMessage("u1", "c1", "m1");
    }

    @Test
    void list_shouldReturnPage() {
        var dto = new MessageDtos.MessageResponse("m1", "c1", "u1", MessageType.TEXT, "hello", null, null, DeliveryStatus.SENT, Instant.now());
        Page<MessageDtos.MessageResponse> page = new PageImpl<>(List.of(dto));
        when(messageService.list(eq("u1"), eq("c1"), any())).thenReturn(page);

        ResponseEntity<Page<MessageDtos.MessageResponse>> response = controller.list("c1", PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        verify(messageService).markAllAsRead("u1", "c1");
    }
}
