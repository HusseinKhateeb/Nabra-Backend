package com.nabra.backend.modules.visualdictionary.service;

import com.nabra.backend.modules.visualdictionary.model.DictionaryEntry;
import com.nabra.backend.modules.visualdictionary.repository.DictionaryEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DictionaryService {

  private final DictionaryEntryRepository dictionaryEntryRepository;

  public Page<DictionaryEntry> search(String q, Pageable pageable) {
    return dictionaryEntryRepository.findByWordContainingIgnoreCaseOrderByWordAsc(q, pageable);
  }

  public Page<DictionaryEntry> byCategory(String category, Pageable pageable) {
    return dictionaryEntryRepository.findByCategoryIgnoreCaseOrderByWordAsc(category, pageable);
  }

  public DictionaryEntry get(String id) {
    return dictionaryEntryRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Dictionary entry not found"));
  }
}
