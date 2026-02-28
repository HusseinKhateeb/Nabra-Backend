package com.nabra.backend.modules.lipreading.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.lipreading.dto.LipReadingDtos;
import com.nabra.backend.modules.lipreading.service.LipReadingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.nio.file.Path;
import java.io.File;

@RestController
@RequestMapping("/api/v1/lipreading")
@RequiredArgsConstructor
@Tag(name = "Lip Reading")
public class LipReadingController {

  private final LipReadingService lipReadingService;
  private static final Logger log = LoggerFactory.getLogger(LipReadingController.class);

  /**
   * Cloud inference endpoint (conditional feature in SRS). Offline inference is
   * done on-device.
   */
  @PostMapping("/infer")
  public ResponseEntity<LipReadingDtos.LipReadingResponse> infer(
      @Valid @RequestBody LipReadingDtos.LipReadingRequest req) {
    // SecurityUtils.currentPrincipal(); // ensure authenticated
    return ResponseEntity.ok(lipReadingService.infer(req));
  }

  @PostMapping("/avsr/fuse")
  public ResponseEntity<LipReadingDtos.AvsrFusionResponse> fuse(@RequestBody LipReadingDtos.AvsrFusionRequest req) {
    // SecurityUtils.currentPrincipal(); // ensure authenticated
    return ResponseEntity.ok(lipReadingService.fuse(req));
  }

  @PostMapping(value = "/avsr/fuse-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> fuseFiles(
      @RequestParam("audioFile") MultipartFile audioFile,
      @RequestParam("videoFile") MultipartFile videoFile) throws Exception {
    log.info("Received fuse-files request");

    if (audioFile == null || audioFile.isEmpty()) {
      throw new IllegalArgumentException("audioFile is required");
    }
    if (videoFile == null || videoFile.isEmpty()) {
      throw new IllegalArgumentException("videoFile is required");
    }

    Path audioTemp = null;
    Path videoTemp = null;
    try {
      audioTemp = Files.createTempFile("fuse-audio-", extensionOf(audioFile.getOriginalFilename()));
      videoTemp = Files.createTempFile("fuse-video-", extensionOf(videoFile.getOriginalFilename()));
      audioFile.transferTo(audioTemp);
      videoFile.transferTo(videoTemp);

        // Run the batch AVSR Python script with positional arguments (fixed path)
        ProcessBuilder pb = new ProcessBuilder(
          "python",
          "avsr_batch_fusion.py",
          audioTemp.toAbsolutePath().toString(),
          videoTemp.toAbsolutePath().toString()
        );
        pb.directory(new File("src/main/java/com/nabra/backend/modules/lipreading/avsrModels/3D ResNet-18"));
        pb.redirectErrorStream(true);
        Process process = pb.start();

      StringBuilder output = new StringBuilder();
      try (java.io.BufferedReader reader = new java.io.BufferedReader(
          new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new RuntimeException("AVSR fusion failed:\n" + output);
      }

      return ResponseEntity.ok(output.toString().trim());
    } finally {
      deleteQuietly(audioTemp);
      deleteQuietly(videoTemp);
    }
  }

  @PostMapping(value = "/avsr/upload-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> uploadAudio(
      @RequestParam("audioFile") MultipartFile audioFile) throws Exception {
    // SecurityUtils.currentPrincipal(); // ensure authenticated
    log.info("Received audio upload request");

    if (audioFile == null || audioFile.isEmpty()) {
      throw new IllegalArgumentException("audioFile is required");
    }

    Path audioTemp = null;
    try {
      audioTemp = Files.createTempFile("avsr-audio-", extensionOf(audioFile.getOriginalFilename()));
      audioFile.transferTo(audioTemp);

      // TODO: Process audio file as needed
      return ResponseEntity.ok("Audio uploaded: " + audioTemp.toAbsolutePath());
    } finally {
      deleteQuietly(audioTemp);
    }
  }

  @PostMapping(value = "/avsr/upload-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> uploadVideo(
      @RequestParam("videoFile") MultipartFile videoFile)
      throws Exception {
    // SecurityUtils.currentPrincipal(); // ensure authenticated
    log.info("Received video upload request");

    if (videoFile == null || videoFile.isEmpty()) {
      throw new IllegalArgumentException("videoFile is required");
    }

    Path videoTemp = null;
    try {
      videoTemp = Files.createTempFile("avsr-video-", extensionOf(videoFile.getOriginalFilename()));
      videoFile.transferTo(videoTemp);

      // TODO: Process video file as needed
      return ResponseEntity.ok("Video uploaded: " + videoTemp.toAbsolutePath());
    } finally {
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
