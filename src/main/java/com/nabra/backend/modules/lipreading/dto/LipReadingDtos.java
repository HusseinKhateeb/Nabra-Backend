package com.nabra.backend.modules.lipreading.dto;

import com.nabra.backend.common.model.Enums.SessionInputType;
import com.nabra.backend.common.model.Enums.SessionOutputType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class LipReadingDtos {

  /**
   * For cloud-based mode: mobile uploads video to object storage and sends videoUrl here.
   */
  public record LipReadingRequest(
      @NotBlank String videoUrl,
      @NotNull SessionInputType inputType,
      @NotNull SessionOutputType outputType,
      String preferredLanguage
  ) {}

  public record LipReadingResponse(
      String recognizedText,
      String synthesizedAudioUrl,
      Double confidence
  ) {}

    public record AvsrTopPrediction(
      @NotBlank String word,
      @NotNull Double confidence
    ) {}

    public record AvsrFusionRequest(
      String audioText,
      List<AvsrTopPrediction> lipTopPredictions,
      String videoPath,
      String audioPath,
      Integer topK
    ) {}

    public record AvsrFusionResponse(
      String finalWord,
      String matchedLipWord,
      String audioText,
      Double similarity,
      Double lipConfidence,
      List<AvsrTopPrediction> lipTopPredictions,
      String fusionReason
    ) {}
}
