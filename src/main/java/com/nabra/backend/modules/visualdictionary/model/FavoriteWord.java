package com.nabra.backend.modules.visualdictionary.model;

import com.nabra.backend.modules.usermanagement.model.User;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "favorite_words")
@Getter
@Setter
public class FavoriteWord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;
}
