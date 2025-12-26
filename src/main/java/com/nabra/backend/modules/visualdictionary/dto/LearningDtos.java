package com.nabra.backend.modules.visualdictionary.dto;

import com.nabra.backend.common.model.Enums.LearningLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class LearningDtos {

  public record LearningProgressResponse(
      String id,
      String userId,
      LearningLevel level,
      long points,
      long completedLessons,
      long completedQuizzes,
      Set<String> favoriteEntryIds
  ) {}

  public record UpdateProgressRequest(
      @NotNull LearningLevel level,
      @Min(0) Long points,
      @Min(0) Long completedLessons,
      @Min(0) Long completedQuizzes
  ) {}
}
