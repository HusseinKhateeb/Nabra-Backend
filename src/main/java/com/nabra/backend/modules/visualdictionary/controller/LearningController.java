package com.nabra.backend.modules.visualdictionary.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.visualdictionary.dto.LearningDtos;
import com.nabra.backend.modules.visualdictionary.service.LearningService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/learning")
@RequiredArgsConstructor
@Tag(name = "Learning")
public class LearningController {

  private final LearningService learningService;

  @GetMapping("/progress")
  public ResponseEntity<LearningDtos.LearningProgressResponse> getProgress() {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(learningService.getProgress(p.getUserId()));
  }

  @PutMapping("/progress")
  public ResponseEntity<LearningDtos.LearningProgressResponse> updateProgress(@Valid @RequestBody LearningDtos.UpdateProgressRequest req) {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(learningService.updateProgress(p.getUserId(), req));
  }

  @PostMapping("/favorites/{entryId}")
  public ResponseEntity<LearningDtos.LearningProgressResponse> addFavorite(@PathVariable String entryId) {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(learningService.addFavorite(p.getUserId(), entryId));
  }

  @DeleteMapping("/favorites/{entryId}")
  public ResponseEntity<LearningDtos.LearningProgressResponse> removeFavorite(@PathVariable String entryId) {
    var p = SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(learningService.removeFavorite(p.getUserId(), entryId));
  }
}
