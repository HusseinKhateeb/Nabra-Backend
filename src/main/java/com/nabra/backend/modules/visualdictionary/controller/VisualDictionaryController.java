package com.nabra.backend.modules.visualdictionary.controller;

import com.nabra.backend.modules.visualdictionary.dto.CategoryWithWordsDto;
import com.nabra.backend.modules.visualdictionary.dto.WordDto;
import com.nabra.backend.modules.visualdictionary.service.VisualDictionaryService;
import org.springframework.http.ResponseEntity;
import com.nabra.backend.security.principal.UserPrincipal; // ✅
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/visual-dictionary")
@RequiredArgsConstructor
public class VisualDictionaryController {

    private final VisualDictionaryService service;

    @GetMapping("/categories")
    public List<CategoryWithWordsDto> categories() {
        return service.getCategoriesWithWords();
    }

    @DeleteMapping("/categories/{categoryId}")
    public ResponseEntity<?> deleteCategory(@PathVariable("categoryId") String categoryId) {
        try {
            service.deleteCategory(categoryId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

@GetMapping("/categories/{categoryId}/words")
public List<WordDto> getWords(
        @PathVariable("categoryId") String categoryId,  // ✅ الحل هنا
        @AuthenticationPrincipal UserPrincipal principal
) {
    String userId = principal != null ? principal.getId() : null;
    return service.getWords(categoryId, userId);
}


    @PostMapping("/favorites/{wordId}")
    public void addFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("wordId") String wordId
    ) {
        service.addFavorite(principal.getId(), wordId);
    }

    @DeleteMapping("/favorites/{wordId}")
    public void removeFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("wordId") String wordId
    ) {
        service.removeFavorite(principal.getId(), wordId);
    }

    @GetMapping("/favorites")
    public List<WordDto> getFavorites(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return service.getFavorites(principal.getId());
    }

    @DeleteMapping("/words/{wordId}")
    public ResponseEntity<?> deleteWord(@PathVariable("wordId") String wordId) {
        try {
            service.deleteWord(wordId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
