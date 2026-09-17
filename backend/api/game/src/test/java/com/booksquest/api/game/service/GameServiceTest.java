package com.booksquest.api.game.service;

import com.booksquest.api.game.util.BookServiceClient;
import com.booksquest.api.game.dto.GameSessionDTO;
import com.booksquest.api.game.entity.GameSession;
import com.booksquest.api.game.exception.BookNotFoundException;
import com.booksquest.api.game.exception.GameSessionNotFoundException;
import com.booksquest.api.game.repository.GameSessionRepository;
import com.booksquest.shared.dto.BookDTO;
import com.booksquest.shared.exception.SectionNotFoundException;
import com.booksquest.shared.testdata.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameService Unit Tests")
class GameServiceTest {

    @Mock
    private GameSessionRepository gameSessionRepository;

    @Mock
    private BookServiceClient bookServiceClient;

    private ObjectMapper objectMapper;

    @InjectMocks
    private GameService gameService;

    private BookDTO testBook;
    private GameSession testGameSession;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        gameService = new GameService(gameSessionRepository, bookServiceClient, objectMapper);
        
        testBook = TestDataBuilder.buildTestBook();

        testGameSession = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .savedSectionId(1)
                .savedHealth(10)
                .activeSectionId(1)
                .activeHealth(10)
                .status("ACTIVE")
                .moveHistory("[]")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ===== CREATE GAME SESSION TESTS =====

    @Test
    @DisplayName("Should create new game session successfully")
    void testCreateGameSession_NewSession_Success() {
        // Given
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.findFirstByUserIdAndBookIdOrderByUpdatedAtDesc(1L, 1L))
                .thenReturn(Optional.empty());
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(testGameSession);

        // When
        GameSessionDTO result = gameService.createGameSession(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getBookId());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(1, result.getActiveSectionId());
        assertEquals(10, result.getActiveHealth());
        verify(bookServiceClient).getBookById(1L);
        verify(gameSessionRepository).save(any(GameSession.class));
    }

    @Test
    @DisplayName("Should throw exception when book does not exist")
    void testCreateGameSession_BookNotFound() {
        // Given
        when(bookServiceClient.getBookById(999L))
                .thenThrow(new RuntimeException("Book not found"));

        // When & Then
        BookNotFoundException exception = assertThrows(BookNotFoundException.class,
                () -> gameService.createGameSession(1L, 999L));

        assertTrue(exception.getMessage().contains("not found"));
        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should resume ACTIVE session when one exists")
    void testCreateGameSession_ResumeActiveSession() {
        // Given
        GameSession existingSession = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .savedSectionId(2)
                .savedHealth(8)
                .activeSectionId(3)
                .activeHealth(6)
                .status("ACTIVE")
                .moveHistory("[1,2]")
                .build();

        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.findFirstByUserIdAndBookIdOrderByUpdatedAtDesc(1L, 1L))
                .thenReturn(Optional.of(existingSession));
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(existingSession);

        // When
        GameSessionDTO result = gameService.createGameSession(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(2, result.getActiveSectionId());
        assertEquals(8, result.getActiveHealth());
    }

    @Test
    @DisplayName("Should resume SAVED session when one exists")
    void testCreateGameSession_ResumeSavedSession() {
        // Given
        GameSession existingSession = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .savedSectionId(4)
                .savedHealth(7)
                .activeSectionId(2)
                .activeHealth(5)
                .status("SAVED")
                .moveHistory("[1]")
                .build();

        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.findFirstByUserIdAndBookIdOrderByUpdatedAtDesc(1L, 1L))
                .thenReturn(Optional.of(existingSession));
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(existingSession);

        // When
        GameSessionDTO result = gameService.createGameSession(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(4, result.getActiveSectionId());
        assertEquals(7, result.getActiveHealth());
    }

    // ===== GET GAME SESSION TESTS =====

    @Test
    @DisplayName("Should retrieve game session for correct user")
    void testGetGameSession_Success() {
        // Given
        when(gameSessionRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(testGameSession));

        // When
        GameSessionDTO result = gameService.getGameSession(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getBookId());
        verify(gameSessionRepository).findByIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("Should throw exception when game session not found")
    void testGetGameSession_NotFound() {
        // Given
        when(gameSessionRepository.findByIdAndUserId(999L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        GameSessionNotFoundException exception = assertThrows(GameSessionNotFoundException.class,
                () -> gameService.getGameSession(999L, 1L));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Should throw exception when user tries to access another user's session")
    void testGetGameSession_UnauthorizedUser() {
        // Given
        when(gameSessionRepository.findByIdAndUserId(1L, 2L))
                .thenReturn(Optional.empty());

        // When & Then
        GameSessionNotFoundException exception = assertThrows(GameSessionNotFoundException.class,
                () -> gameService.getGameSession(1L, 2L));

        assertTrue(exception.getMessage().contains("not found"));
    }

    // ===== MAKE MOVE TESTS =====

    @Test
    @DisplayName("Should successfully make a move to next section")
    void testMakeMove_Success() {
        // Given
        when(gameSessionRepository.findByIdAndUserIdAndBookId(1L, 1L, 1L))
                .thenReturn(Optional.of(testGameSession));
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(testGameSession);

        // When
        GameSessionDTO result = gameService.makeMove(1L, 1L, 2, 1L);

        // Then
        assertNotNull(result);
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getActiveSectionId());
    }

    @Test
    @DisplayName("Should apply health loss consequence correctly")
    void testMakeMove_LoseHealthConsequence() {
        // Given
        GameSession session = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .savedSectionId(2)
                .savedHealth(10)
                .activeSectionId(2)
                .activeHealth(10)
                .status("ACTIVE")
                .moveHistory("[]")
                .build();

        when(gameSessionRepository.findByIdAndUserIdAndBookId(1L, 1L, 1L))
                .thenReturn(Optional.of(session));
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(session);

        // When - Move from section 2 to 3 using Fight option (which causes -3 health)
        gameService.makeMove(1L, 1L, 3, 1L);

        // Then
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getActiveHealth());
    }

    @Test
    @DisplayName("Should apply health gain consequence correctly")
    void testMakeMove_GainHealthConsequence() {
        // Given
        GameSession session = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .savedSectionId(2)
                .savedHealth(10)
                .activeSectionId(2)
                .activeHealth(5)
                .status("ACTIVE")
                .moveHistory("[]")
                .build();

        when(gameSessionRepository.findByIdAndUserIdAndBookId(1L, 1L, 1L))
                .thenReturn(Optional.of(session));
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(session);

        // When - Move from section 2 to 4 using Run away option (which causes +2 health)
        gameService.makeMove(1L, 1L, 4, 1L);

        // Then
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getActiveHealth());
    }

    @Test
    @DisplayName("Should not reduce health below 0")
    void testMakeMove_HealthNotBelowZero() {
        // Given
        GameSession session = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .savedSectionId(2)
                .savedHealth(10)
                .activeSectionId(2)
                .activeHealth(2)
                .status("ACTIVE")
                .moveHistory("[]")
                .build();

        when(gameSessionRepository.findByIdAndUserIdAndBookId(1L, 1L, 1L))
                .thenReturn(Optional.of(session));
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(session);

        // When - Move from section 2 to 3 with Fight option (causes -3 health, would be -1)
        gameService.makeMove(1L, 1L, 3, 1L);

        // Then
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getActiveHealth());
    }

    @Test
    @DisplayName("Should set status to COMPLETED when reaching END section")
    void testMakeMove_ReachingEndSection() {
        // Given
        GameSession session = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .activeSectionId(4)
                .activeHealth(10)
                .status("ACTIVE")
                .moveHistory("[1,2]")
                .build();

        when(gameSessionRepository.findByIdAndUserIdAndBookId(1L, 1L, 1L))
                .thenReturn(Optional.of(session));
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(session);

        // When - Move to section 5 (END section)
        gameService.makeMove(1L, 1L, 5, 1L);

        // Then
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository, atLeast(1)).save(captor.capture());
        assertEquals("COMPLETED", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should set status to COMPLETED when health reaches 0")
    void testMakeMove_HealthReachesZero() {
        // Given
        GameSession session = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .activeSectionId(2)
                .activeHealth(2)
                .status("ACTIVE")
                .moveHistory("[]")
                .build();

        when(gameSessionRepository.findByIdAndUserIdAndBookId(1L, 1L, 1L))
                .thenReturn(Optional.of(session));
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(session);

        // When - Move from section 2 to 3 with Fight option (causes -3 health, kills player)
        gameService.makeMove(1L, 1L, 3, 1L);

        // Then
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository, atLeast(1)).save(captor.capture());
        assertEquals("COMPLETED", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should throw exception for invalid next section")
    void testMakeMove_InvalidSection() {
        // Given
        when(gameSessionRepository.findByIdAndUserIdAndBookId(1L, 1L, 1L))
                .thenReturn(Optional.of(testGameSession));
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);

        // When & Then
        SectionNotFoundException exception = assertThrows(SectionNotFoundException.class,
                () -> gameService.makeMove(1L, 1L, 999, 1L));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Should record move history correctly")
    void testMakeMove_RecordsMoveHistory() {
        // Given
        when(gameSessionRepository.findByIdAndUserIdAndBookId(1L, 1L, 1L))
                .thenReturn(Optional.of(testGameSession));
        when(bookServiceClient.getBookById(1L)).thenReturn(testBook);
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(testGameSession);

        // When
        gameService.makeMove(1L, 1L, 2, 1L);

        // Then
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(captor.capture());
        assertTrue(captor.getValue().getMoveHistory().contains("1"));
    }

    // ===== SAVE GAME TESTS =====

    @Test
    @DisplayName("Should save game successfully")
    void testSaveGame_Success() {
        // Given
        GameSession activeSession = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .activeSectionId(3)
                .activeHealth(7)
                .savedSectionId(1)
                .savedHealth(10)
                .status("ACTIVE")
                .build();

        when(gameSessionRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(activeSession));
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(activeSession);

        // When
        gameService.saveGame(1L, 1L);

        // Then
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getSavedSectionId());
        assertEquals(7, captor.getValue().getSavedHealth());
        assertEquals("SAVED", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should throw exception when saving non-existent session")
    void testSaveGame_NotFound() {
        // Given
        when(gameSessionRepository.findByIdAndUserId(999L, 1L))
                .thenReturn(Optional.empty());

        // When & Then
        GameSessionNotFoundException exception = assertThrows(GameSessionNotFoundException.class,
                () -> gameService.saveGame(999L, 1L));

        assertTrue(exception.getMessage().contains("not found"));
        verify(gameSessionRepository, never()).deleteByUserIdAndBookIdAndStatus(anyLong(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should copy active state to saved state when saving")
    void testSaveGame_CopiesActiveState() {
        // Given
        GameSession session = GameSession.builder()
                .id(1L)
                .userId(1L)
                .bookId(1L)
                .activeSectionId(4)
                .activeHealth(6)
                .savedSectionId(2)
                .savedHealth(9)
                .status("ACTIVE")
                .build();

        when(gameSessionRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(session));
        when(gameSessionRepository.save(any(GameSession.class))).thenReturn(session);

        // When
        gameService.saveGame(1L, 1L);

        // Then
        ArgumentCaptor<GameSession> captor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(captor.capture());
        assertEquals(4, captor.getValue().getSavedSectionId());
        assertEquals(6, captor.getValue().getSavedHealth());
    }
}
