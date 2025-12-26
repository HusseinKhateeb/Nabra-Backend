package com.nabra.backend.modules.visualdictionary.service;

import com.nabra.backend.modules.usermanagement.model.User;
import com.nabra.backend.modules.usermanagement.service.UserService;
import com.nabra.backend.modules.visualdictionary.dto.LearningDtos;
import com.nabra.backend.modules.visualdictionary.model.DictionaryEntry;
import com.nabra.backend.modules.visualdictionary.model.LearningProgress;
import com.nabra.backend.modules.visualdictionary.repository.DictionaryEntryRepository;
import com.nabra.backend.modules.visualdictionary.repository.LearningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningService {

  private final LearningProgressRepository learningProgressRepository;
  private final DictionaryEntryRepository dictionaryEntryRepository;
  private final UserService userService;

  public LearningProgress getOrCreate(String userId) {
    return learningProgressRepository.findByUserId(userId)
        .orElseGet(() -> {
          User u = userService.getById(userId);
          LearningProgress lp = new LearningProgress();
          lp.setUser(u);
          return learningProgressRepository.save(lp);
        });
  }

  public LearningDtos.LearningProgressResponse toDto(LearningProgress lp) {
    Set<String> favIds = lp.getFavorites().stream().map(DictionaryEntry::getId).collect(Collectors.toSet());
    return new LearningDtos.LearningProgressResponse(lp.getId(), lp.getUser().getId(), lp.getLevel(), lp.getPoints(),
        lp.getCompletedLessons(), lp.getCompletedQuizzes(), favIds);
  }

  public LearningDtos.LearningProgressResponse getProgress(String userId) {
    return toDto(getOrCreate(userId));
  }

  public LearningDtos.LearningProgressResponse updateProgress(String userId, LearningDtos.UpdateProgressRequest req) {
    LearningProgress lp = getOrCreate(userId);
    lp.setLevel(req.level());
    if (req.points() != null) lp.setPoints(req.points());
    if (req.completedLessons() != null) lp.setCompletedLessons(req.completedLessons());
    if (req.completedQuizzes() != null) lp.setCompletedQuizzes(req.completedQuizzes());
    lp.setLastUpdatedAt(java.time.Instant.now());
    return toDto(learningProgressRepository.save(lp));
  }

  public LearningDtos.LearningProgressResponse addFavorite(String userId, String entryId) {
    LearningProgress lp = getOrCreate(userId);
    DictionaryEntry entry = dictionaryEntryRepository.findById(entryId)
        .orElseThrow(() -> new IllegalArgumentException("Dictionary entry not found"));
    lp.getFavorites().add(entry);
    lp.setLastUpdatedAt(java.time.Instant.now());
    return toDto(learningProgressRepository.save(lp));
  }

  public LearningDtos.LearningProgressResponse removeFavorite(String userId, String entryId) {
    LearningProgress lp = getOrCreate(userId);
    lp.getFavorites().removeIf(e -> e.getId().equals(entryId));
    lp.setLastUpdatedAt(java.time.Instant.now());
    return toDto(learningProgressRepository.save(lp));
  }
}
