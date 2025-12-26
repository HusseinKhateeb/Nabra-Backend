package com.nabra.backend.modules.chatcommunication.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class VoiceToTextService {

  private final RestClient restClient = RestClient.create();

  @Value("${app.ai.stt.baseUrl:}")
  private String baseUrl;

  public String transcribe(String audioUrl, String preferredLanguage) {
    if (!StringUtils.hasText(baseUrl)) {
      // If no STT configured, return null and let client provide transcript.
      return null;
    }
    Map<String, Object> req = Map.of(
        "audioUrl", audioUrl,
        "preferredLanguage", preferredLanguage == null ? "ar" : preferredLanguage
    );
    Map<?, ?> resp = restClient.post().uri(baseUrl + "/transcribe").body(req).retrieve().body(Map.class);
    if (resp == null) return null;
    Object text = resp.get("text");
    return text == null ? null : text.toString();
  }
}
