package com.nabra.backend.modules.lipreading.controller;

import com.nabra.backend.common.model.Enums.SessionInputType;
import com.nabra.backend.common.model.Enums.SessionOutputType;
import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.lipreading.dto.LipReadingDtos;
import com.nabra.backend.modules.lipreading.service.LipReadingService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LipReadingControllerTest {

    @Mock
    private LipReadingService lipReadingService;

    @Mock
    private SessionService sessionService;

    private LipReadingController controller;
    private MockedStatic<SecurityUtils> securityUtils;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new LipReadingController(lipReadingService, sessionService);
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
    void ping_shouldReturnOk() {
        ResponseEntity<String> response = controller.ping();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Backend is reachable");
    }

    @Test
    void infer_shouldReturnResponse() {
        var req = new LipReadingDtos.LipReadingRequest("http://video.url", SessionInputType.RECORDED, SessionOutputType.TEXT, "ar");
        var expected = new LipReadingDtos.LipReadingResponse("recognized", "http://audio.url", 0.95);
        when(lipReadingService.infer(any())).thenReturn(expected);

        ResponseEntity<LipReadingDtos.LipReadingResponse> response = controller.infer(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(sessionService).recordCompleted(eq("u1"), any(), any(), any(), any(), any(), any());
    }

    @Test
    void infer_whenServiceThrows_shouldPropagate() {
        var req = new LipReadingDtos.LipReadingRequest("http://video.url", SessionInputType.RECORDED, SessionOutputType.TEXT, "ar");
        when(lipReadingService.infer(any())).thenThrow(new RuntimeException("infer failed"));

        assertThrows(RuntimeException.class, () -> controller.infer(req));
        verify(sessionService).recordFailed(eq("u1"), any(), any(), any(), any());
    }

    @Test
    void fuse_shouldReturnResponse() {
        var req = new LipReadingDtos.AvsrFusionRequest("audio text", List.of(), null, null, 10);
        var expected = new LipReadingDtos.AvsrFusionResponse("word", "word", "audio", 0.9, 0.8, List.of(), "matched");
        when(lipReadingService.fuse(any())).thenReturn(expected);

        ResponseEntity<LipReadingDtos.AvsrFusionResponse> response = controller.fuse(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    void fuseFiles_withMissingAudio_shouldThrow() {
        MockMultipartFile audioFile = new MockMultipartFile("audioFile", "test.wav", "audio/wav", new byte[0]);
        MockMultipartFile videoFile = new MockMultipartFile("videoFile", "test.mp4", "video/mp4", new byte[]{1});

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                controller.fuseFiles(audioFile, videoFile, false, 180L, false, null, null));

        assertThat(ex.getMessage()).contains("audioFile is required");
    }

    @Test
    void fuseFiles_withMissingVideo_shouldThrow() {
        MockMultipartFile audioFile = new MockMultipartFile("audioFile", "test.wav", "audio/wav", new byte[]{1});
        MockMultipartFile videoFile = new MockMultipartFile("videoFile", "test.mp4", "video/mp4", new byte[0]);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                controller.fuseFiles(audioFile, videoFile, false, 180L, false, null, null));

        assertThat(ex.getMessage()).contains("videoFile is required");
    }

    @Test
    void fuseFilesStatus_withNonExistentJob_shouldReturnNotFound() throws Exception {
        ResponseEntity<Object> response = controller.fuseFilesStatus("nonexistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(Map.class);
    }

    @Test
    void uploadAudio_withMissingFile_shouldThrow() {
        MockMultipartFile emptyFile = new MockMultipartFile("audioFile", "test.wav", "audio/wav", new byte[0]);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                controller.uploadAudio(emptyFile));

        assertThat(ex.getMessage()).contains("audioFile is required");
    }

    @Test
    void uploadVideo_withMissingFile_shouldThrow() {
        MockMultipartFile emptyFile = new MockMultipartFile("videoFile", "test.mp4", "video/mp4", new byte[0]);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                controller.uploadVideo(emptyFile));

        assertThat(ex.getMessage()).contains("videoFile is required");
    }
}
