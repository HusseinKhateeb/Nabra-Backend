package com.nabra.backend.modules.admintools.controller;

import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import com.nabra.backend.modules.usermanagement.dto.UserDtos;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.repository.UserRepository;
import com.nabra.backend.modules.usermanagement.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private AdminUserController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminUserController(userRepository, userService);
    }

    @Test
    void list_shouldReturnAllUsers() {
        User u = new User();
        u.setId("u1");
        u.setUsername("testuser");
        u.setDisplayName("Test User");
        u.setEmail("test@test.com");
        u.setRole(UserRole.USER);
        u.setStatus(UserStatus.ACTIVE);
        u.setUserType(UserType.OTHER);

        UserDtos.UserProfileResponse dto = new UserDtos.UserProfileResponse("u1", "testuser", "Test User", "test@test.com", null, null, UserRole.USER, UserStatus.ACTIVE, UserType.OTHER, null, null, null, false, 1.0, false, false, null);

        Page<User> userPage = new PageImpl<>(List.of(u));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(userService.toProfile(any())).thenReturn(dto);

        ResponseEntity<Page<UserDtos.UserProfileResponse>> response = controller.list(PageRequest.of(0, 20));

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).id()).isEqualTo("u1");
    }
}
