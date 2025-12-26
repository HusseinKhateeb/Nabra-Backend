package com.nabra.backend.modules.lipreading.service;

import com.nabra.backend.modules.lipreading.dto.LipReadingDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class LipReadingService {

  private final RestClient restClient = RestClient.create();

  @Value("${app.ai.lipreading.baseUrl:}")
  private String baseUrl;

  /**
   * Optional cloud inference. Per SRS, offline inference is on-device, but cloud-based high-accuracy mode is conditional.
   */
  public LipReadingDtos.LipReadingResponse infer(LipReadingDtos.LipReadingRequest req) {
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalArgumentException("Cloud lip-reading is not configured (app.ai.lipreading.baseUrl)");
    }
    return restClient.post()
        .uri(baseUrl + "/predict")
        .body(req)
        .retrieve()
        .body(LipReadingDtos.LipReadingResponse.class);
  }
}
