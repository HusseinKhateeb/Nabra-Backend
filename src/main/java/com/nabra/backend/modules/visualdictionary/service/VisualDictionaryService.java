package com.nabra.backend.modules.visualdictionary.service;

import com.nabra.backend.modules.visualdictionary.dto.*;
import com.nabra.backend.modules.visualdictionary.model.*;
import com.nabra.backend.modules.visualdictionary.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class VisualDictionaryService {

    private final CategoryRepository categoryRepo;
    private final WordRepository wordRepo;
    private final WordVideoRepository videoRepo;
    private final FavoriteWordRepository favoriteRepo;

    private static final String VIDEO_DIR = "uploads/videos/";

    /* =====================
       USER
       ===================== */

    public List<CategoryDto> getCategories() {
        return categoryRepo.findAll().stream().map(c -> {
            CategoryDto dto = new CategoryDto();
            dto.setId(c.getId());
            dto.setName(c.getName());
            dto.setIcon(c.getIcon());
            return dto;
        }).collect(Collectors.toList());
    }

    public List<WordDto> getWords(String categoryId, String userId) {
        return wordRepo.findByCategoryId(categoryId).stream().map(w -> {
            WordDto dto = new WordDto();
            dto.setId(w.getId());
            dto.setText(w.getText());
            dto.setDescription(w.getDescription());
            dto.setVideoUrl(w.getVideo() != null ? w.getVideo().getVideoUrl() : null);

            dto.setFavorite(
                    userId != null &&
                    favoriteRepo.existsByUserIdAndWord_Id(userId, w.getId())
            );

            return dto;
        }).collect(Collectors.toList());
    }

    /* =====================
       FAVORITES ⭐
       ===================== */

    public void addFavorite(String userId, String wordId) {
        if (!favoriteRepo.existsByUserIdAndWord_Id(userId, wordId)) {
            Word word = wordRepo.findById(wordId)
                    .orElseThrow(() -> new RuntimeException("Word not found"));

            FavoriteWord fav = new FavoriteWord();
            fav.setUserId(userId);
            fav.setWord(word);

            favoriteRepo.save(fav);
        }
    }

    public void removeFavorite(String userId, String wordId) {
        favoriteRepo.findByUserId(userId).stream()
                .filter(f -> f.getWord().getId().equals(wordId))
                .findFirst()
                .ifPresent(favoriteRepo::delete);
    }

    public List<WordDto> getFavorites(String userId) {
        return favoriteRepo.findByUserId(userId).stream()
                .map(fav -> {
                    Word w = fav.getWord();
                    WordDto dto = new WordDto();
                    dto.setId(w.getId());
                    dto.setText(w.getText());
                    dto.setDescription(w.getDescription());
                    dto.setVideoUrl(
                        w.getVideo() != null ? w.getVideo().getVideoUrl() : null
                    );
                    dto.setFavorite(true);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /* =====================
       ADMIN
       ===================== */

    public Category createCategory(CreateCategoryRequest req) {
        Category c = new Category();
        c.setName(req.getName());
        c.setIcon(req.getIcon());
        return categoryRepo.save(c);
    }

    public Word createWord(CreateWordRequest req) {
        Category category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Word w = new Word();
        w.setText(req.getText());
        w.setDescription(req.getDescription());
        w.setCategory(category);

        return wordRepo.save(w);
    }

    public WordVideo uploadVideo(String wordId, MultipartFile file) {
        try {
            File dir = new File(VIDEO_DIR);
            if (!dir.exists()) dir.mkdirs();

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File dest = new File(dir, fileName);
            file.transferTo(dest);

            Word word = wordRepo.findById(wordId).orElseThrow();

            WordVideo video = new WordVideo();
            video.setWord(word);
            video.setVideoUrl("/videos/" + fileName);

            return videoRepo.save(video);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
