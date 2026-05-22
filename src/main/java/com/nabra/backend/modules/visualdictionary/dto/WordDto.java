package com.nabra.backend.modules.visualdictionary.dto;

import lombok.Data;

@Data
public class WordDto {
    private String id;
    private String text;
    private String description;
    private String videoUrl;
    private boolean favorite;
}
