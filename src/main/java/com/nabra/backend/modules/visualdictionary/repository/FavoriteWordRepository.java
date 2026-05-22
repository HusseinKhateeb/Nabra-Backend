package com.nabra.backend.modules.visualdictionary.repository;

import com.nabra.backend.modules.visualdictionary.model.FavoriteWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteWordRepository extends JpaRepository<FavoriteWord, String> {

    List<FavoriteWord> findByUserId(String userId);

    // ✅ لاحظ: word_Id وليس wordId
    boolean existsByUserIdAndWord_Id(String userId, String wordId);
}
