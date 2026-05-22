package com.nabra.backend.modules.visualdictionary.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String text; // الكلمة

    private String description; // شرح بسيط (اختياري)

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToOne(mappedBy = "word", cascade = CascadeType.ALL)
    private WordVideo video;
}
