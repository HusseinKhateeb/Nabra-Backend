package com.nabra.backend.modules.visualdictionary.dto;

import lombok.Data;
import java.util.List;

@Data
public class CategoryWithWordsDto {
    private String id;
    private String name;
    private String icon;
    private List<WordDto> words;
}