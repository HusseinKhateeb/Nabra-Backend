package com.nabra.backend.modules.sessionhistory.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nabra.backend.common.model.Enums.SessionInputType;
import com.nabra.backend.common.model.Enums.SessionOutputType;
import com.nabra.backend.common.model.Enums.SessionStatus;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.sessionhistory.dto.SessionDtos;
import com.nabra.backend.modules.sessionhistory.service.SessionService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SessionService sessionService;

    private SessionController controller;
    private MockedStatic<SecurityUtils> securityUtils;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new SessionController(sessionService);
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
    void start_shouldReturnOk() {
        var req = new SessionDtos.StartSessionRequest(SessionInputType.LIVE, SessionOutputType.TEXT, null, null, false);
        var expected = new SessionDtos.SessionResponse("s1", "u1", SessionInputType.LIVE, SessionOutputType.TEXT, SessionStatus.ACTIVE, Instant.now(), null, null, null, null, null, null, null, false, null);
        when(sessionService.start(eq("u1"), any())).thenReturn(expected);

        ResponseEntity<SessionDtos.SessionResponse> response = controller.start(req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void stop_shouldReturnOk() {
        var req = new SessionDtos.StopSessionRequest(null, "result text", null, 0.95);
        var expected = new SessionDtos.SessionResponse("s1", "u1", SessionInputType.LIVE, SessionOutputType.TEXT, SessionStatus.COMPLETED, Instant.now(), Instant.now(), 10L, "result text", null, 0.95, null, null, false, null);
        when(sessionService.stop(eq("u1"), eq("s1"), any())).thenReturn(expected);

        ResponseEntity<SessionDtos.SessionResponse> response = controller.stop("s1", req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void getById_shouldReturnSession() {
        var expected = new SessionDtos.SessionResponse("s1", "u1", SessionInputType.LIVE, SessionOutputType.TEXT, SessionStatus.ACTIVE, Instant.now(), null, null, null, null, null, null, null, false, null);
        when(sessionService.getById("u1", "s1")).thenReturn(expected);

        ResponseEntity<SessionDtos.SessionResponse> response = controller.getById("s1");

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void list_shouldReturnPage() {
        var dto = new SessionDtos.SessionResponse("s1", "u1", SessionInputType.LIVE, SessionOutputType.TEXT, SessionStatus.ACTIVE, Instant.now(), null, null, null, null, null, null, null, false, null);
        Page<SessionDtos.SessionResponse> page = new PageImpl<>(List.of(dto));
        when(sessionService.list(eq("u1"), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        ResponseEntity<Page<SessionDtos.SessionResponse>> response = controller.list(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.delete("s1");

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
        verify(sessionService).deleteById("u1", "s1");
    }

    @Test
    void deleteAll_withoutConfirm_shouldReturnBadRequest() {
        ResponseEntity<Void> response = controller.deleteAll(false);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        verify(sessionService, never()).deleteAllUserSessions(any());
    }

    @Test
    void deleteAll_withConfirm_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteAll(true);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
        verify(sessionService).deleteAllUserSessions("u1");
    }
}
