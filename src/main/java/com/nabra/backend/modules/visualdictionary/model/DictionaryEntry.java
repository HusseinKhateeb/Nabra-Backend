package com.nabra.backend.modules.visualdictionary.model;

import com.nabra.backend.common.model.BaseEntity;
import com.nabra.backend.common.model.Enums.LearningLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dictionary_entries", indexes = {
    @Index(name = "idx_dictionary_word", columnList = "word"),
    @Index(name = "idx_dictionary_category", columnList = "category")
})
@Getter
@Setter
public class DictionaryEntry extends BaseEntity {

  /** Arabic word. */
  @Column(nullable = false, length = 200)
  private String word;

  @Column(length = 200)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LearningLevel difficulty = LearningLevel.BEGINNER;

  /** Lip-movement demo video (S3 URL or CDN). */
  @Column(nullable = false, length = 600)
  private String videoUrl;

  @Column(length = 1000)
  private String description;
}
