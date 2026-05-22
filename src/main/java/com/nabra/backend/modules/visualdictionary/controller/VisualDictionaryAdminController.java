package com.nabra.backend.modules.visualdictionary.controller;
import com.nabra.backend.modules.visualdictionary.model.Category;
import com.nabra.backend.modules.visualdictionary.model.Word;
import com.nabra.backend.modules.visualdictionary.dto.*;
import com.nabra.backend.modules.visualdictionary.model.WordVideo;
import com.nabra.backend.modules.visualdictionary.service.VisualDictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/visual-dictionary")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class VisualDictionaryAdminController {

    private final VisualDictionaryService service;

  @PostMapping("/category")
public CategoryDto createCategory(@RequestBody CreateCategoryRequest req) {
    Category c = service.createCategory(req);

    CategoryDto dto = new CategoryDto();
    dto.setId(c.getId());
    dto.setName(c.getName());
    dto.setIcon(c.getIcon());

    return dto;
}

@PostMapping("/word")
public WordDto createWord(@RequestBody CreateWordRequest req) {
    Word w = service.createWord(req);

    WordDto dto = new WordDto();
    dto.setId(w.getId());
    dto.setText(w.getText());
    dto.setDescription(w.getDescription());
    dto.setVideoUrl(null);
    dto.setFavorite(false);

    return dto;
}

@PostMapping("/word/{wordId}/video")
public WordDto uploadVideo(
        @PathVariable("wordId") String wordId,
        @RequestParam("file") MultipartFile file
) {
    WordVideo video = service.uploadVideo(wordId, file);
    Word w = video.getWord();

    WordDto dto = new WordDto();
    dto.setId(w.getId());
    dto.setText(w.getText());
    dto.setDescription(w.getDescription());
    dto.setVideoUrl(video.getVideoUrl());
    dto.setFavorite(false);

    return dto;
}

}
