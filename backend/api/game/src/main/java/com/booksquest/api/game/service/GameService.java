package com.booksquest.api.game.service;

import com.booksquest.api.game.util.BookServiceClient;
import com.booksquest.api.game.dto.GameSessionDTO;
import com.booksquest.api.game.dto.MoveDTO;
import com.booksquest.api.game.entity.GameSession;
import com.booksquest.api.game.exception.BookNotFoundException;
import com.booksquest.api.game.exception.GameSessionNotFoundException;
import com.booksquest.api.game.repository.GameSessionRepository;
import com.booksquest.shared.dto.BookDTO;
import com.booksquest.shared.exception.SectionNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameSessionRepository gameSessionRepository;
    private final BookServiceClient bookServiceClient;
    private final ObjectMapper objectMapper;

    public GameSessionDTO createGameSession(Long userId, Long bookId) {
        validateBookExists(bookId);
        
        var existingSession = gameSessionRepository.findFirstByUserIdAndBookIdOrderByUpdatedAtDesc(userId, bookId);
        
        if (existingSession.isPresent()) {
            GameSession session = existingSession.get();
            
            // Resume both ACTIVE and SAVED sessions
            if ("ACTIVE".equals(session.getStatus()) || "SAVED".equals(session.getStatus())) {
                session.setStatus("ACTIVE");
                session.setActiveSectionId(session.getSavedSectionId());
                session.setActiveHealth(session.getSavedHealth());
                session.setUpdatedAt(LocalDateTime.now());
                return convertToDTO(gameSessionRepository.save(session));
            }
        }

        // Create new game session
        GameSession gameSession = GameSession.builder()
            .userId(userId)
            .bookId(bookId)
            .savedSectionId(1)
            .savedHealth(10)
            .activeSectionId(1)
            .activeHealth(10)
            .status("ACTIVE")
            .moveHistory(serializeMoveHistory(new ArrayList<>()))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        return convertToDTO(gameSessionRepository.save(gameSession));
    }

    public GameSessionDTO getGameSession(Long gameSessionId, Long userId) {
        GameSession gameSession = gameSessionRepository.findByIdAndUserId(gameSessionId, userId)
            .orElseThrow(() -> new GameSessionNotFoundException("Game session not found with id: " + gameSessionId));
        return convertToDTO(gameSession);
    }

    @Transactional
    public GameSessionDTO makeMove(Long gameSessionId, Long userId, Integer nextSectionId, Long bookId) {
        GameSession gameSession = gameSessionRepository.findByIdAndUserIdAndBookId(gameSessionId, userId, bookId)
            .orElseThrow(() -> new GameSessionNotFoundException("Game session not found"));

        BookDTO bookDTO = bookServiceClient.getBookById(bookId);
        JsonNode bookContent = bookDTO.getContent();
        JsonNode sections = bookContent.get("sections");

        List<Integer> moveHistory = deserializeMoveHistory(gameSession.getMoveHistory());
        moveHistory.add(gameSession.getActiveSectionId());
        gameSession.setMoveHistory(serializeMoveHistory(moveHistory));

        JsonNode currentSection = findSectionById(sections, gameSession.getActiveSectionId());
        int healthChange = 0;

        if (currentSection != null && currentSection.has("options") && currentSection.get("options").isArray()) {
            for (JsonNode option : currentSection.get("options")) {
                JsonNode gotoNode = option.get("gotoId");
                if (gotoNode == null) {
                    continue;
                }
                int gotoId = gotoNode.isNumber() ? gotoNode.asInt() : Integer.parseInt(gotoNode.asText());
                if (gotoId == nextSectionId) {
                    JsonNode consequence = option.get("consequence");
                    if (consequence != null) {
                        JsonNode typeNode = consequence.get("type");
                        JsonNode valueNode = consequence.get("value");
                        if (typeNode != null && valueNode != null) {
                            String consequenceType = typeNode.asText();
                            int value = valueNode.asInt();
                            if ("LOSE_HEALTH".equals(consequenceType)) {
                                healthChange = -value;
                            } else if ("GAIN_HEALTH".equals(consequenceType)) {
                                healthChange = value;
                            }
                        }
                    }
                    break;
                }
            }
        }

        JsonNode nextSection = findSectionById(sections, nextSectionId);
        if (nextSection == null) {
            throw new SectionNotFoundException("Section not found: " + nextSectionId);
        }

        int newActiveHealth = Math.max(0, gameSession.getActiveHealth() + healthChange);
        gameSession.setActiveSectionId(nextSectionId);
        gameSession.setActiveHealth(newActiveHealth);
        gameSession.setUpdatedAt(LocalDateTime.now());

        gameSession = gameSessionRepository.save(gameSession);

        String nextSectionType = nextSection.get("type").asText();
        if ("END".equals(nextSectionType) || newActiveHealth <= 0) {
            gameSession.setStatus("COMPLETED");
            gameSessionRepository.deleteByUserIdAndBookIdAndStatus(userId, bookId, "SAVED");
        }

        return convertToDTO(gameSession);
    }

    public void saveGame(Long gameSessionId, Long userId) {
        GameSession gameSession = gameSessionRepository.findByIdAndUserId(gameSessionId, userId)
            .orElseThrow(() -> new GameSessionNotFoundException("Game session not found"));

        // Copy active state to saved state
        gameSession.setSavedSectionId(gameSession.getActiveSectionId());
        gameSession.setSavedHealth(gameSession.getActiveHealth());
        gameSession.setStatus("SAVED");
        gameSession.setUpdatedAt(LocalDateTime.now());
        gameSessionRepository.save(gameSession);
    }

    private JsonNode findSectionById(JsonNode sections, Integer sectionId) {
        for (JsonNode section : sections) {
            JsonNode idNode = section.get("id");
            int currentId = idNode.isNumber() ? idNode.asInt() : Integer.parseInt(idNode.asText());
            if (currentId == sectionId) {
                return section;
            }
        }
        return null;
    }

    private void validateBookExists(Long bookId) {
        try {
            bookServiceClient.getBookById(bookId);
        } catch (RuntimeException ex) {
            throw new BookNotFoundException("Book not found with id: " + bookId);
        }
    }

    private GameSessionDTO convertToDTO(GameSession gameSession) {
        return GameSessionDTO.builder()
            .id(gameSession.getId())
            .bookId(gameSession.getBookId())
            .savedSectionId(gameSession.getSavedSectionId())
            .savedHealth(gameSession.getSavedHealth())
            .activeSectionId(gameSession.getActiveSectionId())
            .activeHealth(gameSession.getActiveHealth())
            .status(gameSession.getStatus())
            .moveHistory(deserializeMoveHistory(gameSession.getMoveHistory()))
            .createdAt(gameSession.getCreatedAt())
            .updatedAt(gameSession.getUpdatedAt())
            .build();
    }

    private String serializeMoveHistory(List<Integer> moveHistory) {
        try {
            return objectMapper.writeValueAsString(moveHistory);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<Integer> deserializeMoveHistory(String moveHistory) {
        try {
            if (moveHistory == null || moveHistory.isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(moveHistory, objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
