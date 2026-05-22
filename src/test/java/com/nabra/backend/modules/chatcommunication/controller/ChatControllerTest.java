package com.nabra.backend.modules.chatcommunication.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.chatcommunication.dto.ChatDtos;
import com.nabra.backend.modules.chatcommunication.service.ChatService;
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

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    private ChatController controller;
    private MockedStatic<SecurityUtils> securityUtils;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatService);
        User user = new User();
        user.setId("u1");
        principal = new UserPrincipal(user);
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::currentPrincipal).thenReturn(principal);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void create_shouldReturnOk() {
        var req = new ChatDtos.CreateChatRequest(false, "test chat", Set.of("u2"));
        var expected = new ChatDtos.ChatResponse("c1", false, "test chat", Set.of(), null, "", 0);
        when(chatService.create(eq("u1"), any())).thenReturn(expected);

        ResponseEntity<ChatDtos.ChatResponse> response = controller.create(req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void list_shouldReturnPage() {
        var dto = new ChatDtos.ChatResponse("c1", false, "test", Set.of(), null, "", 0);
        Page<ChatDtos.ChatResponse> page = new PageImpl<>(List.of(dto));
        when(chatService.listForUser(eq("u1"), any())).thenReturn(page);

        ResponseEntity<Page<ChatDtos.ChatResponse>> response = controller.list(PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.delete("c1");

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
        verify(chatService).delete("u1", "c1");
    }
}
