package com.nabra.backend.modules.smartprediction.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.smartprediction.dto.PredictionDtos;
import com.nabra.backend.modules.smartprediction.service.PredictionService;
import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.security.principal.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PredictionControllerTest {

    @Mock
    private PredictionService predictionService;

    private PredictionController controller;
    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUp() {
        controller = new PredictionController(predictionService);
        User user = new User();
        user.setId("u1");
        UserPrincipal principal = new UserPrincipal(user);
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::currentPrincipal).thenReturn(principal);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void next_shouldReturnSuggestions() {
        var req = new PredictionDtos.NextWordRequest("hello world", "en");
        var expected = new PredictionDtos.NextWordResponse(List.of("suggestion1", "suggestion2"));
        when(predictionService.next(any())).thenReturn(expected);

        ResponseEntity<PredictionDtos.NextWordResponse> response = controller.next(req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void correct_shouldReturnCorrection() {
        var req = new PredictionDtos.CorrectRequest("helo", "en");
        var expected = new PredictionDtos.CorrectResponse("hello", 0.95);
        when(predictionService.correct(any())).thenReturn(expected);

        ResponseEntity<PredictionDtos.CorrectResponse> response = controller.correct(req);

        assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }
}
