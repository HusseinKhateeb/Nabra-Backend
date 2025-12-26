package com.nabra.backend.modules.visualdictionary.dto;

import com.nabra.backend.common.model.Enums.LearningLevel;

public class DictionaryDtos {
  public record DictionaryEntryResponse(
      String id,
      String word,
      String category,
      LearningLevel difficulty,
      String videoUrl,
      String description
  ) {}
}
