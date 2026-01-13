package com.nabra.backend.modules.visualdictionary.controller;

import com.nabra.backend.modules.visualdictionary.dto.CategoryDto;
import com.nabra.backend.modules.visualdictionary.dto.WordDto;
import com.nabra.backend.modules.visualdictionary.service.VisualDictionaryService;
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
    public List<CategoryDto> categories() {
        return service.getCategories();
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
}
