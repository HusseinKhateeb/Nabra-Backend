package com.nabra.backend.modules.visualdictionary.repository;

import com.nabra.backend.modules.visualdictionary.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, String> {

    List<Word> findByCategoryId(String categoryId);
}
