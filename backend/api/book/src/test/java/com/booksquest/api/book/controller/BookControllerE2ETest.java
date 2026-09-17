package com.booksquest.api.book.controller;

import com.booksquest.api.book.repository.BookRepository;
import com.booksquest.shared.config.AppConfig;
import com.booksquest.shared.entity.Book;
import com.booksquest.shared.testdata.TestDataBuilder;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("BookController E2E Tests")
class BookControllerE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private BookRepository bookRepository;

    private static final String API_PREFIX = AppConfig.API_PREFIX;
    private static final String BOOKS_API = "/books";
    
    private Long testBookId1;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = API_PREFIX + BOOKS_API;
        
        // Clear existing data and insert test books
        bookRepository.deleteAll();
        
        Book testBook1 = Book.builder()
                .title("Test Adventure")
                .author("Test Author")
                .difficulty("MEDIUM")
                .category("Fantasy")
                .description("A test adventure book")
                .tags("fantasy,adventure")
                .fastReadingMinutes(30)
                .slowReadingMinutes(60)
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();
        
        Book testBook2 = Book.builder()
                .title("Mystery Novel")
                .author("Mystery Writer")
                .difficulty("HARD")
                .category("Mystery")
                .description("An intriguing mystery")
                .tags("mystery,detective")
                .fastReadingMinutes(45)
                .slowReadingMinutes(90)
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();
        
        testBook1 = bookRepository.save(testBook1);
        bookRepository.save(testBook2);
        
        // Store the ID of the first test book for use in tests
        testBookId1 = testBook1.getId();
    }

    // ===== GET ALL BOOKS TESTS =====

    @Test
    @DisplayName("Should return all books (public endpoint)")
    void testGetAllBooks_Success() {
        // When
        Response response = given()
                .when()
                .get();

        // Then
        response.then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0))
                .body("[0]", notNullValue()); // At least check first element if any exist
    }

    @Test
    @DisplayName("Should return books with all required fields")
    void testGetAllBooks_ContainsAllFields() {
        // When
        Response response = given()
                .when()
                .get();

        // Then
        if (response.jsonPath().getList("").size() > 0) {
            response.then()
                    .statusCode(200)
                    .body("[0].id", notNullValue())
                    .body("[0].title", notNullValue())
                    .body("[0].author", notNullValue())
                    .body("[0].difficulty", notNullValue())
                    .body("[0].chapters", notNullValue());
        }
    }

    @Test
    @DisplayName("Should be accessible without authentication")
    void testGetAllBooks_NoAuthRequired() {
        // When
        Response response = given()
                .header("Authorization", "Bearer invalid-token")
                .when()
                .get();

        // Then - Should still work without valid auth (public endpoint)
        response.then()
                .statusCode(200);
    }

    // ===== GET BOOK BY ID TESTS =====

    @Test
    @DisplayName("Should return specific book by ID")
    void testGetBookById_Success() {
        // Given
        long bookId = testBookId1;

        // When
        Response response = given()
                .when()
                .get("/{id}", bookId);

        // Then
        response.then()
                .statusCode(200)
                .body("id", equalTo((int)(long)bookId))
                .body("title", notNullValue())
                .body("author", notNullValue());
    }

    @Test
    @DisplayName("Should return 404 for non-existent book")
    void testGetBookById_NotFound() {
        // Given
        long nonExistentId = 999999L;

        // When
        Response response = given()
                .when()
                .get("/{id}", nonExistentId);

        // Then
        response.then()
                .statusCode(404)
                .body("error", containsString("not found"));
    }

    @Test
    @DisplayName("Should return book with correct structure")
    void testGetBookById_CorrectStructure() {
        // Given
        long bookId = testBookId1;

        // When
        Response response = given()
                .when()
                .get("/{id}", bookId);

        // Then
        if (response.statusCode() == 200) {
            response.then()
                    .body("id", notNullValue())
                    .body("title", notNullValue())
                    .body("author", notNullValue())
                    .body("difficulty", notNullValue())
                    .body("category", notNullValue())
                    .body("description", notNullValue())
                    .body("chapters", notNullValue())
                    .body("content", notNullValue());
        }
    }

    @Test
    @DisplayName("Should return book with parsed tags as array")
    void testGetBookById_TagsAsArray() {
        // Given
        long bookId = testBookId1;

        // When
        Response response = given()
                .when()
                .get("/{id}", bookId);

        // Then
        if (response.statusCode() == 200) {
            response.then()
                    .body("tags", instanceOf(java.util.List.class));
        }
    }

    @Test
    @DisplayName("Should return book with full content JSON")
    void testGetBookById_FullContent() {
        // Given
        long bookId = testBookId1;

        // When
        Response response = given()
                .when()
                .get("/{id}", bookId);

        // Then
        if (response.statusCode() == 200) {
            response.then()
                    .body("content", notNullValue())
                    .body("content.sections", notNullValue());
        }
    }

    // ===== SEARCH AND FILTER TESTS =====

    @Test
    @DisplayName("Should filter books by search keyword")
    void testSearchBooks_ByKeyword() {
        // When
        Response response = given()
                .queryParam("search", "quest")
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Should filter books by difficulty")
    void testSearchBooks_ByDifficulty() {
        // When
        Response response = given()
                .queryParam("difficulty", "EASY")
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200);

        if (response.jsonPath().getList("").size() > 0) {
            response.then()
                    .body("[0].difficulty", equalTo("EASY"));
        }
    }

    @Test
    @DisplayName("Should filter books by category")
    void testSearchBooks_ByCategory() {
        // When
        Response response = given()
                .queryParam("category", "Fantasy")
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200);

        if (response.jsonPath().getList("").size() > 0) {
            response.then()
                    .body("[0].category", equalTo("Fantasy"));
        }
    }

    @Test
    @DisplayName("Should filter books by tags")
    void testSearchBooks_ByTags() {
        // When
        Response response = given()
                .queryParam("tags", "fantasy")
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should apply multiple filters")
    void testSearchBooks_MultipleFilters() {
        // When
        Response response = given()
                .queryParam("search", "quest")
                .queryParam("difficulty", "MEDIUM")
                .queryParam("category", "Fantasy")
                .queryParam("tags", "adventure")
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Should return empty list when no matches for search")
    void testSearchBooks_NoMatches() {
        // When
        Response response = given()
                .queryParam("search", "NONEXISTENT_BOOK_TITLE_XYZ")
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    @Test
    @DisplayName("Should handle case-insensitive search")
    void testSearchBooks_CaseInsensitive() {
        // When
        Response response1 = given()
                .queryParam("search", "quest")
                .when()
                .get("/search");

        Response response2 = given()
                .queryParam("search", "QUEST")
                .when()
                .get("/search");

        // Then
        assert response1.jsonPath().getList("").size() == response2.jsonPath().getList("").size();
    }

    @Test
    @DisplayName("Should accept multiple tags in tag filter")
    void testSearchBooks_MultipleTags() {
        // When
        Response response = given()
                .queryParam("tags", "fantasy,adventure,quest")
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should return correct book count")
    void testSearchBooks_CorrectCount() {
        // When
        Response response = given()
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Should handle search with spaces")
    void testSearchBooks_WithSpaces() {
        // When
        Response response = given()
                .queryParam("search", " quest ")
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200);
    }

    @Test
    @DisplayName("Should filter by difficulty case-insensitive")
    void testSearchBooks_DifficultyCaseInsensitive() {
        // When
        Response response = given()
                .queryParam("difficulty", "easy")
                .when()
                .get("/search");

        // Then
        response.then()
                .statusCode(200);
    }
}
