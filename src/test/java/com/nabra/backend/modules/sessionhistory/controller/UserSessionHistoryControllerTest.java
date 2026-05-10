package com.nabra.backend.modules.sessionhistory.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSessionHistoryControllerTest {

    @Mock
    private SessionService sessionService;

    private UserSessionHistoryController controller;
    private MockedStatic<SecurityUtils> securityUtils;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new UserSessionHistoryController(sessionService);
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
    void listAvsrHistory_shouldReturnPage() {
        var dto = new SessionDtos.SessionResponse("s1", "u1", SessionInputType.LIVE, SessionOutputType.TEXT, SessionStatus.ACTIVE, Instant.now(), null, null, null, null, null, null, null, false, null);
        Page<SessionDtos.SessionResponse> page = new PageImpl<>(List.of(dto));
        when(sessionService.listAvsrHistory(eq("u1"), eq("u1"), any())).thenReturn(page);

        ResponseEntity<Page<SessionDtos.SessionResponse>> response = controller.listAvsrHistory("u1", PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }
}
