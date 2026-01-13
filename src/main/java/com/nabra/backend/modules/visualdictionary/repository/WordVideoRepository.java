package com.nabra.backend.modules.visualdictionary.repository;

import com.nabra.backend.modules.visualdictionary.model.WordVideo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordVideoRepository extends JpaRepository<WordVideo, String> {
}
