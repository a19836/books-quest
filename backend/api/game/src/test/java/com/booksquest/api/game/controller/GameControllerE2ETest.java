package com.booksquest.api.game.controller;

import com.booksquest.api.game.util.BookServiceClient;
import com.booksquest.api.game.dto.MoveDTO;
import com.booksquest.shared.config.AppConfig;
import com.booksquest.shared.dto.BookDTO;
import com.booksquest.shared.util.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("GameController E2E Tests")
class GameControllerE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtProvider jwtProvider;

    @MockBean
    private BookServiceClient bookServiceClient;

    private static final String API_PREFIX = AppConfig.API_PREFIX;
    private static final String GAMES_API = "/games";
    private static final long TEST_BOOK_ID = 1L;
    private static final String TEST_EMAIL_TEMPLATE = "test%d@example.com";

    private String authToken;
    private Long testUserId;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = API_PREFIX + GAMES_API;

        // Generate unique user ID for each test
        testUserId = System.nanoTime();
        
        // Generate auth token directly without calling auth API
        authToken = jwtProvider.generateToken(testUserId, String.format(TEST_EMAIL_TEMPLATE, testUserId));

        // Mock BookServiceClient to return test book data for valid book ID
        BookDTO testBook = createTestBook();
        when(bookServiceClient.getBookById(TEST_BOOK_ID)).thenReturn(testBook);

        // Mock to throw exception for non-existent book ID
        when(bookServiceClient.getBookById(999999L)).thenThrow(new RuntimeException("Book not found"));
    }

    private BookDTO createTestBook() {
        String bookContent = """
                {
                    "sections": [
                        {
                            "id": 1,
                            "type": "NORMAL",
                            "title": "Start",
                            "text": "You begin your adventure.",
                            "options": [
                                {
                                    "id": 1,
                                    "text": "Go left",
                                    "gotoId": "2",
                                    "consequence": {
                                        "type": "LOSE_HEALTH",
                                        "value": 3
                                    }
                                },
                                {
                                    "id": 2,
                                    "text": "Go right",
                                    "gotoId": "3",
                                    "consequence": {
                                        "type": "GAIN_HEALTH",
                                        "value": 2
                                    }
                                }
                            ]
                        },
                        {
                            "id": 2,
                            "type": "NORMAL",
                            "title": "Left path",
                            "text": "You took the left path.",
                            "options": [
                                {
                                    "id": 1,
                                    "text": "Continue",
                                    "gotoId": "4"
                                }
                            ]
                        },
                        {
                            "id": 3,
                            "type": "NORMAL",
                            "title": "Right path",
                            "text": "You took the right path.",
                            "options": [
                                {
                                    "id": 1,
                                    "text": "Continue",
                                    "gotoId": "5"
                                }
                            ]
                        },
                        {
                            "id": 4,
                            "type": "NORMAL",
                            "title": "Middle",
                            "text": "You are in the middle.",
                            "options": [
                                {
                                    "id": 1,
                                    "text": "Continue",
                                    "gotoId": "5"
                                }
                            ]
                        },
                        {
                            "id": 5,
                            "type": "END",
                            "title": "End",
                            "text": "You reached the end.",
                            "options": []
                        }
                    ]
                }
                """;

        try {
            BookDTO book = BookDTO.builder()
                    .id(TEST_BOOK_ID)
                    .title("Test Book")
                    .description("Test Description")
                    .content(objectMapper.readTree(bookContent))
                    .build();
            return book;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test book", e);
        }
    }

    // ===== START GAME TESTS =====

    @Test
    @DisplayName("Should create new game session successfully")
    void testStartGame_Success() {
        // When
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/start/{bookId}", TEST_BOOK_ID);

        // Then
        response.then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("bookId", equalTo((int)TEST_BOOK_ID))
                .body("status", equalTo("ACTIVE"))
                .body("activeSectionId", equalTo(1))
                .body("activeHealth", equalTo(10))
                .body("savedSectionId", equalTo(1))
                .body("savedHealth", equalTo(10));
    }

    @Test
    @DisplayName("Should return 401 when starting game without authentication")
    void testStartGame_NoAuth() {
        // When
        Response response = given()
                .when()
                .post("/start/{bookId}", TEST_BOOK_ID);

        // Then
        response.then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should return 401 when starting game with invalid token")
    void testStartGame_InvalidToken() {
        // When
        Response response = given()
                .header("Authorization", "Bearer invalid-token")
                .when()
                .post("/start/{bookId}", TEST_BOOK_ID);

        // Then
        response.then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should return 404 when starting game with non-existent book")
    void testStartGame_BookNotFound() {
        // When
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/start/{bookId}", 999999L);

        // Then
        response.then()
                .statusCode(404)
                .body("error", containsString("not found"));
    }

    @Test
    @DisplayName("Should resume existing game session if one exists")
    void testStartGame_ResumeExisting() {
        // Given - Start first game
        Response firstStart = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/start/{bookId}", TEST_BOOK_ID);

        Object idObj = firstStart.then()
                .statusCode(201)
                .extract()
                .path("id");
        Long gameSessionId = ((Number) idObj).longValue();

        // When - Try to start same game again
        Response secondStart = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/start/{bookId}", TEST_BOOK_ID);

        // Then - Should return same or resumed session
        secondStart.then()
                .statusCode(201)
                .body("bookId", equalTo((int)TEST_BOOK_ID))
                .body("status", equalTo("ACTIVE"));
    }

    // ===== GET GAME SESSION TESTS =====

    @Test
    @DisplayName("Should retrieve game session successfully")
    void testGetGameSession_Success() {
        // Given
        Long gameSessionId = createGameSession();

        // When
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/{gameSessionId}", gameSessionId);

        // Then
        response.then()
                .statusCode(200)
                .body("id", equalTo(gameSessionId.intValue()))
                .body("bookId", equalTo((int)TEST_BOOK_ID))
                .body("status", notNullValue())
                .body("activeSectionId", notNullValue())
                .body("activeHealth", notNullValue());
    }

    @Test
    @DisplayName("Should return 401 when getting session without authentication")
    void testGetGameSession_NoAuth() {
        // When
        Response response = given()
                .when()
                .get("/{gameSessionId}", 1L);

        // Then
        response.then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should return 404 when getting non-existent session")
    void testGetGameSession_NotFound() {
        // When
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/{gameSessionId}", 999999L);

        // Then
        response.then()
                .statusCode(404)
                .body("error", containsString("not found"));
    }

    // ===== MAKE MOVE TESTS =====

    @Test
    @DisplayName("Should make a move successfully")
    void testMakeMove_Success() {
        // Given
        Long gameSessionId = createGameSession();
        MoveDTO moveDTO = new MoveDTO();
        moveDTO.setNextSectionId(2);

        // When
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body(moveDTO)
                .queryParam("bookId", TEST_BOOK_ID)
                .when()
                .post("/{gameSessionId}/move", gameSessionId);

        // Then
        response.then()
                .statusCode(200)
                .body("activeSectionId", equalTo(2))
                .body("status", equalTo("ACTIVE"));
    }

    @Test
    @DisplayName("Should return 401 when making move without authentication")
    void testMakeMove_NoAuth() {
        // Given
        MoveDTO moveDTO = new MoveDTO();
        moveDTO.setNextSectionId(2);

        // When
        Response response = given()
                .contentType(ContentType.JSON)
                .body(moveDTO)
                .queryParam("bookId", TEST_BOOK_ID)
                .when()
                .post("/{gameSessionId}/move", 1L);

        // Then
        response.then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should apply health consequences when making move")
    void testMakeMove_AppliesHealthConsequence() {
        // Given
        Long gameSessionId = createGameSession();
        MoveDTO moveDTO = new MoveDTO();
        moveDTO.setNextSectionId(2); // This option causes -3 health

        // When
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body(moveDTO)
                .queryParam("bookId", TEST_BOOK_ID)
                .when()
                .post("/{gameSessionId}/move", gameSessionId);

        // Then
        response.then()
                .statusCode(200)
                .body("activeHealth", equalTo(7)); // 10 - 3
    }

    @Test
    @DisplayName("Should complete game when reaching END section")
    void testMakeMove_ReachingEnd() {
        // Given
        Long gameSessionId = createGameSession();

        // Make moves to reach the END section (section 5)
        makeMove(gameSessionId, 2);
        makeMove(gameSessionId, 3);
        makeMove(gameSessionId, 5);

        MoveDTO moveDTO = new MoveDTO();
        moveDTO.setNextSectionId(5);

        // When
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body(moveDTO)
                .queryParam("bookId", TEST_BOOK_ID)
                .when()
                .post("/{gameSessionId}/move", gameSessionId);

        // Then
        response.then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));
    }

    // ===== SAVE GAME TESTS =====

    @Test
    @DisplayName("Should save game successfully")
    void testSaveGame_Success() {
        // Given
        Long gameSessionId = createGameSession();

        // Make a move first
        MoveDTO moveDTO = new MoveDTO();
        moveDTO.setNextSectionId(2);
        given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body(moveDTO)
                .queryParam("bookId", TEST_BOOK_ID)
                .when()
                .post("/{gameSessionId}/move", gameSessionId)
                .then()
                .statusCode(200);

        // When
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/{gameSessionId}/save", gameSessionId);

        // Then
        response.then()
                .statusCode(200);

        // Verify saved state
        Response getResponse = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/{gameSessionId}", gameSessionId);

        getResponse.then()
                .statusCode(200)
                .body("status", equalTo("SAVED"));
    }

    @Test
    @DisplayName("Should return 401 when saving without authentication")
    void testSaveGame_NoAuth() {
        // When
        Response response = given()
                .when()
                .post("/{gameSessionId}/save", 1L);

        // Then
        response.then()
                .statusCode(401);
    }

    @Test
    @DisplayName("Should return 404 when saving non-existent session")
    void testSaveGame_NotFound() {
        // When
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/{gameSessionId}/save", 999999L);

        // Then
        response.then()
                .statusCode(404);
    }

    // ===== COMPLETE GAME FLOW TESTS =====

    @Test
    @DisplayName("Should complete full game flow: start → move → save → resume")
    void testCompleteGameFlow() {
        // 1. Start game
        Long gameSessionId = createGameSession();

        Response startResponse = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/{gameSessionId}", gameSessionId);

        startResponse.then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"))
                .body("activeSectionId", equalTo(1))
                .body("activeHealth", equalTo(10));

        // 2. Make a move
        MoveDTO moveDTO = new MoveDTO();
        moveDTO.setNextSectionId(2);
        Response moveResponse = given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body(moveDTO)
                .queryParam("bookId", TEST_BOOK_ID)
                .when()
                .post("/{gameSessionId}/move", gameSessionId);

        moveResponse.then()
                .statusCode(200)
                .body("activeSectionId", equalTo(2));

        // 3. Save game
        Response saveResponse = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/{gameSessionId}/save", gameSessionId);

        saveResponse.then()
                .statusCode(200);

        // 4. Retrieve saved game
        Response resumeResponse = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .get("/{gameSessionId}", gameSessionId);

        resumeResponse.then()
                .statusCode(200)
                .body("status", equalTo("SAVED"))
                .body("savedSectionId", equalTo(2));
    }

    // ===== HELPER METHODS =====

    private Long createGameSession() {
        Response response = given()
                .header("Authorization", "Bearer " + authToken)
                .when()
                .post("/start/{bookId}", TEST_BOOK_ID);

        Object id = response.then()
                .statusCode(201)
                .extract()
                .path("id");
        return ((Number) id).longValue();
    }

    private void makeMove(Long gameSessionId, Integer nextSectionId) {
        MoveDTO moveDTO = new MoveDTO();
        moveDTO.setNextSectionId(nextSectionId);

        given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body(moveDTO)
                .queryParam("bookId", TEST_BOOK_ID)
                .when()
                .post("/{gameSessionId}/move", gameSessionId)
                .then()
                .statusCode(200);
    }
}
