package com.nabra.backend.modules.visualdictionary.repository;

import com.nabra.backend.modules.visualdictionary.model.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, String> {
  Optional<LearningProgress> findByUserId(String userId);
}
