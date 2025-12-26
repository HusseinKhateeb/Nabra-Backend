package com.nabra.backend.modules.visualdictionary.controller;

import com.nabra.backend.common.web.SecurityUtils;
import com.nabra.backend.modules.visualdictionary.dto.DictionaryDtos;
import com.nabra.backend.modules.visualdictionary.model.DictionaryEntry;
import com.nabra.backend.modules.visualdictionary.service.DictionaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dictionary")
@RequiredArgsConstructor
@Tag(name = "Visual Dictionary")
public class DictionaryController {

  private final DictionaryService dictionaryService;

  private static DictionaryDtos.DictionaryEntryResponse toDto(DictionaryEntry e) {
    return new DictionaryDtos.DictionaryEntryResponse(e.getId(), e.getWord(), e.getCategory(), e.getDifficulty(), e.getVideoUrl(), e.getDescription());
  }

  @GetMapping
  public ResponseEntity<Page<DictionaryDtos.DictionaryEntryResponse>> search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String category,
      Pageable pageable
  ) {
    SecurityUtils.currentPrincipal();

    Page<DictionaryEntry> page;
    if (category != null && !category.isBlank()) {
      page = dictionaryService.byCategory(category, pageable);
    } else if (q != null && !q.isBlank()) {
      page = dictionaryService.search(q, pageable);
    } else {
      page = dictionaryService.search("", pageable);
    }
    return ResponseEntity.ok(page.map(DictionaryController::toDto));
  }

  @GetMapping("/{id}")
  public ResponseEntity<DictionaryDtos.DictionaryEntryResponse> get(@PathVariable String id) {
    SecurityUtils.currentPrincipal();
    return ResponseEntity.ok(toDto(dictionaryService.get(id)));
  }
}
