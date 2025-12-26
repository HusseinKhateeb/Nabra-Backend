package com.nabra.backend.modules.visualdictionary.model;

import com.nabra.backend.common.model.BaseEntity;
import com.nabra.backend.common.model.Enums.LearningLevel;
import com.nabra.backend.modules.usermanagement.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "learning_progress", uniqueConstraints = {
    @UniqueConstraint(name = "uq_learning_progress_user", columnNames = "user_id")
})
@Getter
@Setter
public class LearningProgress extends BaseEntity {

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LearningLevel level = LearningLevel.BEGINNER;

  @Column(nullable = false)
  private long points = 0;

  @Column(nullable = false)
  private long completedLessons = 0;

  @Column(nullable = false)
  private long completedQuizzes = 0;

  @ManyToMany
  @JoinTable(
      name = "favorite_dictionary_entries",
      joinColumns = @JoinColumn(name = "learning_progress_id"),
      inverseJoinColumns = @JoinColumn(name = "dictionary_entry_id")
  )
  private Set<DictionaryEntry> favorites = new HashSet<>();

  @Column(nullable = false)
  private Instant lastUpdatedAt = Instant.now();
}
