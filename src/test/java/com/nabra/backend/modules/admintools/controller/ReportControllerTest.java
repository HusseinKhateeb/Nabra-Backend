package com.nabra.backend.modules.admintools.controller;

import com.nabra.backend.common.model.Enums.ReportStatus;
import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.admintools.dto.ReportDtos;
import com.nabra.backend.modules.admintools.repository.ReportRepository;
import com.nabra.backend.modules.chatcommunication.repository.ChatRepository;
import com.nabra.backend.modules.chatcommunication.repository.MessageRepository;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.service.UserService;
import com.nabra.backend.security.principal.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserService userService;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private MessageRepository messageRepository;

    private ReportController controller;
    private MockedStatic<SecurityUtils> securityUtils;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new ReportController(reportRepository, userService, chatRepository, messageRepository);
        User user = new User();
        user.setId("u1");
        user.setUsername("reporter");
        user.setDisplayName("Reporter");
        user.setEmail("reporter@test.com");
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
    void create_shouldReturnOk() {
        User reportedUser = new User();
        reportedUser.setId("u2");

        when(userService.getById("u1")).thenReturn(principal.getUser());
        when(userService.getById("u2")).thenReturn(reportedUser);
        when(reportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var req = new ReportDtos.CreateReportRequest("u2", null, null, "harassment", "details here");
        ResponseEntity<ReportDtos.ReportResponse> response = controller.create(req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().reason()).isEqualTo("harassment");
        verify(reportRepository).save(any());
    }
}
