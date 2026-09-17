package com.booksquest.api.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSessionDTO {
    private Long id;
    private Long bookId;
    private Integer savedSectionId;
    private Integer savedHealth;
    private Integer activeSectionId;
    private Integer activeHealth;
    private String status;
    private List<Integer> moveHistory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
