package com.nabra.backend.modules.visualdictionary.repository;

import com.nabra.backend.modules.visualdictionary.model.DictionaryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictionaryEntryRepository extends JpaRepository<DictionaryEntry, String> {
  Page<DictionaryEntry> findByWordContainingIgnoreCaseOrderByWordAsc(String q, Pageable pageable);
  Page<DictionaryEntry> findByCategoryIgnoreCaseOrderByWordAsc(String category, Pageable pageable);
}
