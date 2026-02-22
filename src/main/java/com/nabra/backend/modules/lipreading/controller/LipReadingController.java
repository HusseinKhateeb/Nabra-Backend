package com.nabra.backend.modules.lipreading.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.lipreading.dto.LipReadingDtos;
import com.nabra.backend.modules.lipreading.service.LipReadingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/lipreading")
@RequiredArgsConstructor
@Tag(name = "Lip Reading")
public class LipReadingController {

  private final LipReadingService lipReadingService;

  /**
   * Cloud inference endpoint (conditional feature in SRS). Offline inference is done on-device.
   */
  @PostMapping("/infer")
  public ResponseEntity<LipReadingDtos.LipReadingResponse> infer(@Valid @RequestBody LipReadingDtos.LipReadingRequest req) {
    SecurityUtils.currentPrincipal(); // ensure authenticated
    return ResponseEntity.ok(lipReadingService.infer(req));
  }

  @PostMapping("/avsr/fuse")
  public ResponseEntity<LipReadingDtos.AvsrFusionResponse> fuse(@RequestBody LipReadingDtos.AvsrFusionRequest req) {
    SecurityUtils.currentPrincipal(); // ensure authenticated
    return ResponseEntity.ok(lipReadingService.fuse(req));
  }

  @PostMapping(value = "/avsr/fuse-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<LipReadingDtos.AvsrFusionResponse> fuseFiles(
      @RequestParam("audioFile") MultipartFile audioFile,
      @RequestParam("videoFile") MultipartFile videoFile,
      @RequestParam(value = "topK", required = false) Integer topK
  ) throws Exception {
    SecurityUtils.currentPrincipal(); // ensure authenticated

    if (audioFile == null || audioFile.isEmpty()) {
      throw new IllegalArgumentException("audioFile is required");
    }
    if (videoFile == null || videoFile.isEmpty()) {
      throw new IllegalArgumentException("videoFile is required");
    }

    Path audioTemp = null;
    Path videoTemp = null;
    try {
      audioTemp = Files.createTempFile("avsr-audio-", extensionOf(audioFile.getOriginalFilename()));
      videoTemp = Files.createTempFile("avsr-video-", extensionOf(videoFile.getOriginalFilename()));

      audioFile.transferTo(audioTemp);
      videoFile.transferTo(videoTemp);

      LipReadingDtos.AvsrFusionRequest req = new LipReadingDtos.AvsrFusionRequest(
          null,
          null,
          videoTemp.toAbsolutePath().toString(),
          audioTemp.toAbsolutePath().toString(),
          topK
      );

      return ResponseEntity.ok(lipReadingService.fuse(req));
    } finally {
      deleteQuietly(audioTemp);
      deleteQuietly(videoTemp);
    }
  }

  private void deleteQuietly(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (Exception ignored) {
    }
  }

  private String extensionOf(String fileName) {
    if (fileName == null) {
      return ".tmp";
    }
    String normalized = fileName.trim().toLowerCase(Locale.ROOT);
    int dot = normalized.lastIndexOf('.');
    if (dot <= -1 || dot == normalized.length() - 1) {
      return ".tmp";
    }
    return normalized.substring(dot);
  }
}
