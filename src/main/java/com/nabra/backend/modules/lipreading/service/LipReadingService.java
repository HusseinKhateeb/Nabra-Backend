package com.nabra.backend.modules.lipreading.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nabra.backend.modules.lipreading.dto.LipReadingDtos;

import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LipReadingService {

  private final RestClient restClient = RestClient.create();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final Pattern RANKED_LIP_PATTERN = Pattern.compile("\\d+\\.\\s*(.+?)\\s*\\((\\d+(?:\\.\\d+)?)%\\)");

  @Value("${app.ai.lipreading.baseUrl:}")
  private String baseUrl;

  @Value("${app.ai.avsr.pythonCommand:python}")
  private String pythonCommand;

  @Value("${app.ai.avsr.lip.command:}")
  private String lipCommand;

  @Value("${app.ai.avsr.audio.command:}")
  private String audioCommand;

  @Value("${app.ai.avsr.topK:5}")
  private Integer defaultTopK;

  @Value("${app.ai.avsr.timeoutSeconds:60}")
  private Long timeoutSeconds;

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

  public String runAvsrUnified(Path audioPath, Path videoPath) {
    // Ensure commands are configured
    if (!StringUtils.hasText(lipCommand) || !StringUtils.hasText(audioCommand)) {
        throw new IllegalArgumentException("AVSR commands are not configured (app.ai.avsr.lip.command, app.ai.avsr.audio.command)");
    }

    // Build command for AVSR fusion (example, adapt as needed)
    String command = String.format("%s --audio %s --video %s",
            pythonCommand,
            quoteIfNeeded(audioPath.toAbsolutePath().toString()),
            quoteIfNeeded(videoPath.toAbsolutePath().toString())
    );

    // Run the command and capture output
    String output = runCommand(command, "AVSR fusion failed");

    // TODO: Parse output as needed. For now, return raw output.
    return output;
}

  public LipReadingDtos.AvsrFusionResponse fuse(LipReadingDtos.AvsrFusionRequest req) {
    int topK = req.topK() == null || req.topK() < 1 ? defaultTopK : req.topK();

    List<LipReadingDtos.AvsrTopPrediction> lipTopPredictions = sanitizeTopPredictions(req.lipTopPredictions(), topK);
    if (lipTopPredictions.isEmpty() && StringUtils.hasText(req.videoPath()) && StringUtils.hasText(lipCommand)) {
      lipTopPredictions = runLipModel(req.videoPath(), topK);
    }

    String audioText = sanitizeRecognizedText(req.audioText());
    if (!StringUtils.hasText(audioText) && StringUtils.hasText(req.audioPath()) && StringUtils.hasText(audioCommand)) {
      audioText = runAudioModel(req.audioPath());
    }

    if (!StringUtils.hasText(audioText)) {
      throw new IllegalArgumentException("Audio text is missing. Provide audioText, or configure app.ai.avsr.audio.command and pass audioPath.");
    }
    if (lipTopPredictions.isEmpty()) {
      throw new IllegalArgumentException("Lip top predictions are missing. Provide lipTopPredictions, or configure app.ai.avsr.lip.command and pass videoPath.");
    }

    MatchResult bestMatch = findBestMatch(audioText, lipTopPredictions);
    String reason = bestMatch.perfectMatch()
        ? "Exact normalized match between audio text and one top lip prediction."
        : "Selected highest similarity between audio text and lip top predictions.";

    return new LipReadingDtos.AvsrFusionResponse(
        bestMatch.matchedWord(),
        bestMatch.matchedWord(),
        audioText,
        bestMatch.similarity(),
        bestMatch.lipConfidence(),
        lipTopPredictions,
        reason
    );
  }

  private List<LipReadingDtos.AvsrTopPrediction> runLipModel(String videoPath, int topK) {
    String command = applyTemplate(lipCommand, Map.of(
        "python", pythonCommand,
        "video", videoPath,
        "topK", String.valueOf(topK)
    ));
    String output = runCommand(command, "Lip model failed");

    List<LipReadingDtos.AvsrTopPrediction> fromJson = parseLipPredictionsFromJson(output, topK);
    if (!fromJson.isEmpty()) {
      return fromJson;
    }

    List<LipReadingDtos.AvsrTopPrediction> fromText = parseLipPredictionsFromText(output, topK);
    if (!fromText.isEmpty()) {
      return fromText;
    }

    throw new IllegalArgumentException("Could not parse lip model output. Make sure command returns top predictions in JSON or ranked text lines.");
  }

  private String runAudioModel(String audioPath) {
    String command = applyTemplate(audioCommand, Map.of(
        "python", pythonCommand,
        "audio", audioPath
    ));
    String output = runCommand(command, "Audio model failed");

    String jsonText = parseAudioTextFromJson(output);
    if (StringUtils.hasText(jsonText)) {
      return sanitizeRecognizedText(jsonText);
    }

    String lineText = parseAudioTextFromLines(output);
    if (StringUtils.hasText(lineText)) {
      return sanitizeRecognizedText(lineText);
    }

    throw new IllegalArgumentException("Could not parse audio model output. Make sure command returns text in JSON or ASR lines.");
  }

  private String runCommand(String command, String failPrefix) {
    try {
      ProcessBuilder processBuilder;
      if (isWindows()) {
        processBuilder = new ProcessBuilder("cmd", "/c", command);
      } else {
        processBuilder = new ProcessBuilder("sh", "-c", command);
      }
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();

      StringBuilder output = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append('\n');
        }
      }

      boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IllegalArgumentException(failPrefix + ": command timed out after " + timeoutSeconds + " seconds");
      }
      if (process.exitValue() != 0) {
        throw new IllegalArgumentException(failPrefix + ":\n" + output);
      }
      return output.toString();
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalArgumentException(failPrefix + ": " + ex.getMessage(), ex);
    }
  }

  private List<LipReadingDtos.AvsrTopPrediction> sanitizeTopPredictions(List<LipReadingDtos.AvsrTopPrediction> source, int topK) {
    if (source == null || source.isEmpty()) {
      return List.of();
    }
    List<LipReadingDtos.AvsrTopPrediction> result = new ArrayList<>();
    for (LipReadingDtos.AvsrTopPrediction prediction : source) {
      if (prediction == null || !StringUtils.hasText(prediction.word()) || prediction.confidence() == null) {
        continue;
      }
      result.add(new LipReadingDtos.AvsrTopPrediction(normalizeSpaces(prediction.word()), prediction.confidence()));
      if (result.size() >= topK) {
        break;
      }
    }
    return result;
  }

  private List<LipReadingDtos.AvsrTopPrediction> parseLipPredictionsFromJson(String output, int topK) {
    String jsonBody = extractLastJsonObject(output);
    if (!StringUtils.hasText(jsonBody)) {
      return List.of();
    }
    try {
      JsonNode root = objectMapper.readTree(jsonBody);
      JsonNode predictionsNode = root.path("topPredictions");
      if (predictionsNode.isMissingNode() || !predictionsNode.isArray()) {
        predictionsNode = root.path("predictions");
      }
      if (predictionsNode.isMissingNode() || !predictionsNode.isArray()) {
        return List.of();
      }

      List<LipReadingDtos.AvsrTopPrediction> result = new ArrayList<>();
      for (JsonNode node : predictionsNode) {
        String word = normalizeSpaces(node.path("word").asText(""));
        double confidence = node.path("confidence").asDouble(-1.0);
        if (!StringUtils.hasText(word)) {
          continue;
        }
        if (confidence < 0 && node.has("score")) {
          confidence = node.path("score").asDouble(0.0);
        }
        result.add(new LipReadingDtos.AvsrTopPrediction(word, confidence));
        if (result.size() >= topK) {
          break;
        }
      }
      return result;
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private List<LipReadingDtos.AvsrTopPrediction> parseLipPredictionsFromText(String output, int topK) {
    if (!StringUtils.hasText(output)) {
      return List.of();
    }

    List<LipReadingDtos.AvsrTopPrediction> predictions = new ArrayList<>();
    String[] lines = output.split("\\R");
    for (String line : lines) {
      Matcher matcher = RANKED_LIP_PATTERN.matcher(line.trim());
      if (!matcher.find()) {
        continue;
      }
      String word = normalizeSpaces(matcher.group(1));
      double confidence = Double.parseDouble(matcher.group(2));
      predictions.add(new LipReadingDtos.AvsrTopPrediction(word, confidence));
      if (predictions.size() >= topK) {
        break;
      }
    }
    return predictions;
  }

  private String parseAudioTextFromJson(String output) {
    String jsonBody = extractLastJsonObject(output);
    if (!StringUtils.hasText(jsonBody)) {
      return null;
    }
    try {
      Map<String, Object> root = objectMapper.readValue(jsonBody, new TypeReference<>() {});
      Object text = firstNonNull(root.get("audioText"), root.get("text"), root.get("asrText"), root.get("recognizedText"));
      return text == null ? null : String.valueOf(text);
    } catch (Exception ignored) {
      return null;
    }
  }

  private String extractLastJsonObject(String output) {
    if (!StringUtils.hasText(output)) {
      return null;
    }
    String text = output.trim();
    int end = text.lastIndexOf('}');
    if (end < 0) {
      return null;
    }

    for (int start = text.lastIndexOf('{', end); start >= 0; start = text.lastIndexOf('{', start - 1)) {
      String candidate = text.substring(start, end + 1).trim();
      try {
        objectMapper.readTree(candidate);
        return candidate;
      } catch (Exception ignored) {
      }
    }
    return null;
  }

  private String parseAudioTextFromLines(String output) {
    if (!StringUtils.hasText(output)) {
      return null;
    }
    String best = null;
    for (String line : output.split("\\R")) {
      String trimmed = line.trim();
      if (!StringUtils.hasText(trimmed)) {
        continue;
      }
      if (trimmed.toLowerCase(Locale.ROOT).startsWith("asr (arabic):")) {
        String candidate = sanitizeRecognizedText(trimmed.substring("ASR (Arabic):".length()).trim());
        if (StringUtils.hasText(candidate)) {
          return candidate;
        }
        continue;
      }
      String candidate = sanitizeRecognizedText(trimmed);
      if (StringUtils.hasText(candidate)) {
        best = candidate;
      }
    }
    return best;
  }

  private MatchResult findBestMatch(String audioText, List<LipReadingDtos.AvsrTopPrediction> topPredictions) {
    String normalizedAudio = normalizeForSimilarity(audioText);
    if (!StringUtils.hasText(normalizedAudio)) {
      return new MatchResult(topPredictions.get(0).word(), 0.0, topPredictions.get(0).confidence(), false);
    }

    List<MatchResult> results = new ArrayList<>();
    for (LipReadingDtos.AvsrTopPrediction prediction : topPredictions) {
      String normalizedLipWord = normalizeForSimilarity(prediction.word());
      double similarity = similarity(normalizedAudio, normalizedLipWord);
      boolean perfectMatch = normalizedAudio.equals(normalizedLipWord);
      results.add(new MatchResult(prediction.word(), similarity, prediction.confidence(), perfectMatch));
    }

    return results.stream()
        .max(Comparator.comparingDouble(MatchResult::similarity).thenComparingDouble(MatchResult::lipConfidence))
        .orElse(new MatchResult(topPredictions.get(0).word(), 0.0, topPredictions.get(0).confidence(), false));
  }

  private String applyTemplate(String template, Map<String, String> values) {
    if (!StringUtils.hasText(template)) {
      return template;
    }
    Map<String, String> escapedValues = new HashMap<>();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      String value = entry.getValue() == null ? "" : entry.getValue();
      escapedValues.put(entry.getKey(), quoteIfNeeded(value));
    }

    String result = template;
    for (Map.Entry<String, String> entry : escapedValues.entrySet()) {
      result = result.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return result;
  }

  private String quoteIfNeeded(String value) {
    if (!StringUtils.hasText(value)) {
      return value;
    }
    if ((value.startsWith("\"") && value.endsWith("\"")) || !value.contains(" ")) {
      return value;
    }
    return "\"" + value + "\"";
  }

  private String normalizeSpaces(String value) {
    if (value == null) {
      return null;
    }
    return value.trim().replaceAll("\\s+", " ");
  }

  private String sanitizeRecognizedText(String value) {
    if (!StringUtils.hasText(value)) {
      return value;
    }
    return normalizeSpaces(value)
        .replaceAll("^[^\\p{L}\\p{N}]+", "")
        .replaceAll("[^\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF0-9\\s]+", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private String normalizeForSimilarity(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    normalized = normalized
        .replaceAll("[\\u064B-\\u0652\\u0670\\u0640]", "")
        .replaceAll("[^\\p{L}\\p{N}\\s]", "")
        .replaceAll("\\s+", " ")
        .trim();
    return normalized;
  }

  private double similarity(String left, String right) {
    if (!StringUtils.hasText(left) && !StringUtils.hasText(right)) {
      return 1.0;
    }
    if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
      return 0.0;
    }

    int distance = levenshteinDistance(left, right);
    int maxLength = Math.max(left.length(), right.length());
    return maxLength == 0 ? 1.0 : Math.max(0.0, 1.0 - ((double) distance / maxLength));
  }

  private int levenshteinDistance(String source, String target) {
    int sourceLength = source.length();
    int targetLength = target.length();

    int[] previous = new int[targetLength + 1];
    int[] current = new int[targetLength + 1];

    for (int targetIndex = 0; targetIndex <= targetLength; targetIndex++) {
      previous[targetIndex] = targetIndex;
    }

    for (int sourceIndex = 1; sourceIndex <= sourceLength; sourceIndex++) {
      current[0] = sourceIndex;
      char sourceChar = source.charAt(sourceIndex - 1);
      for (int targetIndex = 1; targetIndex <= targetLength; targetIndex++) {
        int cost = sourceChar == target.charAt(targetIndex - 1) ? 0 : 1;
        current[targetIndex] = Math.min(
            Math.min(current[targetIndex - 1] + 1, previous[targetIndex] + 1),
            previous[targetIndex - 1] + cost
        );
      }

      int[] swap = previous;
      previous = current;
      current = swap;
    }
    return previous[targetLength];
  }

  private boolean isWindows() {
    String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    return osName.contains("win");
  }

  @SafeVarargs
  private static <T> T firstNonNull(T... values) {
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private record MatchResult(
      String matchedWord,
      double similarity,
      double lipConfidence,
      boolean perfectMatch
  ) {}
}
