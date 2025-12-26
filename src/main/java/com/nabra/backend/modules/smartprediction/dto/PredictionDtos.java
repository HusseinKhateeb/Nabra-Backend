package com.nabra.backend.modules.smartprediction.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class PredictionDtos {

  public record NextWordRequest(
      @NotBlank String contextText,
      String preferredLanguage
  ) {}

  public record NextWordResponse(
      List<String> suggestions
  ) {}

  public record CorrectRequest(
      @NotBlank String text,
      String preferredLanguage
  ) {}

  public record CorrectResponse(
      String correctedText,
      Double confidence
  ) {}
}
