package com.nabra.backend.modules.visualdictionary.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name; // مثال: الطعام، المشاعر

    private String icon; // optional (للواجهة)

    @OneToMany(mappedBy = "category")
    private Set<Word> words;
}
