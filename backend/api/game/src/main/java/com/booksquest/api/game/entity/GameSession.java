package com.booksquest.api.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity(name = "GameModuleGameSession")
@Table(name = "game_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long bookId;

    // Saved state (only updated when user clicks save)
    @Column(nullable = false)
    private Integer savedSectionId;

    @Column(nullable = false)
    private Integer savedHealth;

    // Active state (current gameplay, updated on every move)
    @Column(nullable = false)
    private Integer activeSectionId;

    @Column(nullable = false)
    private Integer activeHealth;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String moveHistory;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
