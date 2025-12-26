package com.nabra.backend.modules.smartprediction.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.smartprediction.dto.PredictionDtos;
import com.nabra.backend.modules.smartprediction.service.PredictionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prediction")
@RequiredArgsConstructor
@Tag(name = "Smart Prediction")
public class PredictionController {

  private final PredictionService predictionService;

  @PostMapping("/next")
  public ResponseEntity<PredictionDtos.NextWordResponse> next(@Valid @RequestBody PredictionDtos.NextWordRequest req) {
    SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(predictionService.next(req));
  }

  @PostMapping("/correct")
  public ResponseEntity<PredictionDtos.CorrectResponse> correct(@Valid @RequestBody PredictionDtos.CorrectRequest req) {
    SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(predictionService.correct(req));
  }
}
