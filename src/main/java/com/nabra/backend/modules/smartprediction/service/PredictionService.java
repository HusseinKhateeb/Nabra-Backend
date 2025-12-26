package com.nabra.backend.modules.smartprediction.service;

import com.nabra.backend.modules.smartprediction.dto.PredictionDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class PredictionService {

  private final RestClient restClient = RestClient.create();

  @Value("${app.ai.prediction.baseUrl:}")
  private String baseUrl;

  public PredictionDtos.NextWordResponse next(PredictionDtos.NextWordRequest req) {
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalArgumentException("Prediction service is not configured (app.ai.prediction.baseUrl)");
    }
    return restClient.post().uri(baseUrl + "/next").body(req).retrieve().body(PredictionDtos.NextWordResponse.class);
  }

  public PredictionDtos.CorrectResponse correct(PredictionDtos.CorrectRequest req) {
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalArgumentException("Prediction service is not configured (app.ai.prediction.baseUrl)");
    }
    return restClient.post().uri(baseUrl + "/correct").body(req).retrieve().body(PredictionDtos.CorrectResponse.class);
  }
}
