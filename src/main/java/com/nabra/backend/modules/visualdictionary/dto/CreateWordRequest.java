package com.nabra.backend.modules.visualdictionary.dto;

import lombok.Data;

@Data
public class CreateWordRequest {
    private String text;
    private String description;
    private String categoryId;
}
