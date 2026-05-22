package com.nabra.backend.modules.visualdictionary.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class WordVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String videoUrl; // رابط الفيديو (S3 / Local / CDN)

    @OneToOne
    @JoinColumn(name = "word_id")
    private Word word;
}
