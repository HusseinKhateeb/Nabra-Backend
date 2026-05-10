package com.nabra.backend.modules.usermanagement.controller;

import com.nabra.backend.common.model.Enums.UserRole;
import com.nabra.backend.common.model.Enums.UserStatus;
import com.nabra.backend.common.model.Enums.UserType;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.usermanagement.dto.UserDtos;
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

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private UserController controller;
    private MockedStatic<SecurityUtils> securityUtils;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new UserController(userService);
        User user = new User();
        user.setId("u1");
        user.setUsername("testuser");
        user.setDisplayName("Test User");
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
    void me_shouldReturnProfile() {
        var expected = new UserDtos.UserProfileResponse("u1", "testuser", "Test User", "test@test.com", null, null, UserRole.USER, UserStatus.ACTIVE, UserType.OTHER, null, null, null, false, 1.0, false, false, null);
        when(userService.toProfile(any())).thenReturn(expected);

        ResponseEntity<UserDtos.UserProfileResponse> response = controller.me();

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void updateMe_shouldReturnUpdatedProfile() {
        var req = new UserDtos.UpdateProfileRequest("Updated Name", null, null, null, null, null, null, null);
        var expected = new UserDtos.UserProfileResponse("u1", "testuser", "Updated Name", "test@test.com", null, null, UserRole.USER, UserStatus.ACTIVE, UserType.OTHER, null, null, null, false, 1.0, false, false, null);
        when(userService.updateProfile(eq("u1"), any())).thenReturn(expected);

        ResponseEntity<UserDtos.UserProfileResponse> response = controller.updateMe(req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void listUsers_shouldExcludeCurrentUser() {
        User other = new User();
        other.setId("u2");
        other.setDisplayName("Other User");
        when(userService.findAll()).thenReturn(List.of(other));

        List<UserDtos.UserListItem> result = controller.listUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("u2");
    }

    @Test
    void getProfile_shouldReturnProfile() {
        var expected = new UserDtos.UserProfileResponse("u1", "testuser", "Test User", "test@test.com", null, null, UserRole.USER, UserStatus.ACTIVE, UserType.OTHER, null, null, null, false, 1.0, false, false, null);
        when(userService.getProfile("u1")).thenReturn(expected);

        ResponseEntity<UserDtos.UserProfileResponse> response = controller.getProfile();

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void getSettings_shouldReturnSettings() {
        var expected = new UserDtos.UserSettingsResponse("u1", "ar", false, 1.0, true, true, false, true, true, "basic");
        when(userService.getSettings("u1")).thenReturn(expected);

        ResponseEntity<UserDtos.UserSettingsResponse> response = controller.getSettings();

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void updateSettings_shouldReturnUpdatedSettings() {
        var req = new UserDtos.UpdateSettingsRequest("en", true, 1.5, false, null, null, null, null, null);
        var expected = new UserDtos.UserSettingsResponse("u1", "en", true, 1.5, false, true, false, true, true, "basic");
        when(userService.updateSettings(eq("u1"), any())).thenReturn(expected);

        ResponseEntity<UserDtos.UserSettingsResponse> response = controller.updateSettings(req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }
}
