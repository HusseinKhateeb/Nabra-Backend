package com.nabra.backend.modules.visualdictionary.repository;

import com.nabra.backend.modules.visualdictionary.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, String> {
}
