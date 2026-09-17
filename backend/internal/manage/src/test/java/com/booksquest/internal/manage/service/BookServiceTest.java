package com.booksquest.internal.manage.service;

import com.booksquest.internal.manage.exception.BookValidationException;
import com.booksquest.internal.manage.repository.BookRepository;
import com.booksquest.internal.manage.util.BookValidator;
import com.booksquest.shared.dto.BookDTO;
import com.booksquest.shared.entity.Book;
import com.booksquest.shared.testdata.TestDataBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Manage BookService Unit Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    private ObjectMapper objectMapper;

    @InjectMocks
    private BookService bookService;

    private String validBookJson;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        bookService = new BookService(bookRepository, objectMapper);
        validBookJson = TestDataBuilder.buildTestBookJsonString();
    }

    // ===== CREATE BOOK TESTS =====

    @Test
    @DisplayName("Should create book with valid JSON successfully")
    void testCreateBook_ValidJson_Success() {
        // Given
        Book expectedBook = Book.builder()
                .id(1L)
                .title("Test Adventure")
                .author("Test Author")
                .difficulty("MEDIUM")
                .category("Fantasy")
                .description("A test adventure book")
                .tags("fantasy,adventure")
                .fastReadingMinutes(30)
                .slowReadingMinutes(60)
                .jsonContent(validBookJson)
                .build();

        when(bookRepository.save(any(Book.class))).thenReturn(expectedBook);

        // When
        BookDTO result = bookService.createBook(validBookJson);

        // Then
        assertNotNull(result);
        assertEquals("Test Adventure", result.getTitle());
        assertEquals("Test Author", result.getAuthor());
        assertEquals("MEDIUM", result.getDifficulty());
        assertEquals("Fantasy", result.getCategory());

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertEquals("Test Adventure", captor.getValue().getTitle());
    }

    @Test
    @DisplayName("Should throw exception when book JSON has no sections")
    void testCreateBook_NoSections() throws Exception {
        // Given
        JsonNode invalidContent = TestDataBuilder.buildInvalidBookContent_NoSections();
        String invalidJson = objectMapper.writeValueAsString(invalidContent);

        // When & Then
        BookValidationException exception = assertThrows(BookValidationException.class,
                () -> bookService.createBook(invalidJson));

        assertTrue(exception.getMessage().contains("sections"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when book JSON has no BEGIN section")
    void testCreateBook_NoBeginSection() throws Exception {
        // Given
        JsonNode invalidContent = TestDataBuilder.buildInvalidBookContent_NoBegin();
        String invalidJson = objectMapper.writeValueAsString(invalidContent);

        // When & Then
        BookValidationException exception = assertThrows(BookValidationException.class,
                () -> bookService.createBook(invalidJson));

        assertTrue(exception.getMessage().contains("BEGIN"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when book JSON has no END section")
    void testCreateBook_NoEndSection() throws Exception {
        // Given
        JsonNode invalidContent = TestDataBuilder.buildInvalidBookContent_NoEnd();
        String invalidJson = objectMapper.writeValueAsString(invalidContent);

        // When & Then
        BookValidationException exception = assertThrows(BookValidationException.class,
                () -> bookService.createBook(invalidJson));

        assertTrue(exception.getMessage().contains("END"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when non-END section has no options")
    void testCreateBook_MissingOptions() throws Exception {
        // Given
        JsonNode invalidContent = TestDataBuilder.buildInvalidBookContent_NoOptions();
        String invalidJson = objectMapper.writeValueAsString(invalidContent);

        // When & Then
        BookValidationException exception = assertThrows(BookValidationException.class,
                () -> bookService.createBook(invalidJson));

        assertTrue(exception.getMessage().contains("options"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid JSON")
    void testCreateBook_InvalidJson() {
        // Given
        String invalidJson = "{invalid json content";

        // When & Then
        assertThrows(RuntimeException.class, () -> bookService.createBook(invalidJson));
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should parse tags correctly when creating book")
    void testCreateBook_ParsesTags() {
        // Given
        Book savedBook = Book.builder()
                .id(1L)
                .title("Test Adventure")
                .author("Test Author")
                .difficulty("MEDIUM")
                .tags("fantasy,adventure,quest")
                .jsonContent(validBookJson)
                .build();

        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        // When
        BookDTO result = bookService.createBook(validBookJson);

        // Then
        assertNotNull(result.getTags());
        assertEquals(3, result.getTags().size());
    }

    @Test
    @DisplayName("Should set optional fields when present")
    void testCreateBook_OptionalFields() {
        // Given
        Book savedBook = Book.builder()
                .id(1L)
                .title("Test Adventure")
                .author("Test Author")
                .difficulty("MEDIUM")
                .category("Fantasy")
                .description("A test adventure book")
                .fastReadingMinutes(30)
                .slowReadingMinutes(60)
                .jsonContent(validBookJson)
                .build();

        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        // When
        BookDTO result = bookService.createBook(validBookJson);

        // Then
        assertEquals("Fantasy", result.getCategory());
        assertEquals("A test adventure book", result.getDescription());
        assertEquals(30, result.getFastReadingMinutes());
        assertEquals(60, result.getSlowReadingMinutes());
    }

    @Test
    @DisplayName("Should handle books with no tags")
    void testCreateBook_NoTags() {
        // Given
        String bookJsonNoTags = """
                {
                    "title": "Book Without Tags",
                    "author": "Author",
                    "difficulty": "EASY",
                    "sections": [
                        {
                            "id": 1,
                            "type": "BEGIN",
                            "text": "Start",
                            "options": [{"text": "Continue", "gotoId": 2}]
                        },
                        {
                            "id": 2,
                            "type": "END",
                            "text": "End"
                        }
                    ]
                }
                """;

        Book savedBook = Book.builder()
                .id(1L)
                .title("Book Without Tags")
                .author("Author")
                .difficulty("EASY")
                .jsonContent(bookJsonNoTags)
                .build();

        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        // When
        BookDTO result = bookService.createBook(bookJsonNoTags);

        // Then
        assertNotNull(result);
        assertTrue(result.getTags().isEmpty());
    }

    @Test
    @DisplayName("Should count chapters correctly from sections")
    void testCreateBook_CountsChapters() {
        // Given
        Book savedBook = Book.builder()
                .id(1L)
                .title("Test Adventure")
                .author("Test Author")
                .difficulty("MEDIUM")
                .jsonContent(validBookJson)
                .build();

        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        // When
        BookDTO result = bookService.createBook(validBookJson);

        // Then
        assertEquals(5, result.getChapters());
    }

    @Test
    @DisplayName("Should handle JSON with minimal fields")
    void testCreateBook_MinimalFields() {
        // Given
        String minimalJson = """
                {
                    "title": "Minimal Book",
                    "author": "Author",
                    "difficulty": "MEDIUM",
                    "sections": [
                        {
                            "id": 1,
                            "type": "BEGIN",
                            "text": "Start",
                            "options": [{"text": "Continue", "gotoId": 2}]
                        },
                        {
                            "id": 2,
                            "type": "END",
                            "text": "End"
                        }
                    ]
                }
                """;

        Book savedBook = Book.builder()
                .id(1L)
                .title("Minimal Book")
                .author("Author")
                .difficulty("MEDIUM")
                .jsonContent(minimalJson)
                .build();

        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        // When
        BookDTO result = bookService.createBook(minimalJson);

        // Then
        assertNotNull(result);
        assertEquals("Minimal Book", result.getTitle());
        assertNull(result.getCategory());
        assertNull(result.getDescription());
    }

    @Test
    @DisplayName("Should validate book structure before saving")
    void testCreateBook_ValidatesStructure() {
        // Given
        String invalidJson = """
                {
                    "title": "Invalid",
                    "author": "Author",
                    "difficulty": "MEDIUM",
                    "sections": [
                        {
                            "id": 1,
                            "type": "BEGIN",
                            "text": "Start",
                            "options": [{"text": "Go to invalid", "gotoId": 999}]
                        },
                        {
                            "id": 2,
                            "type": "END",
                            "text": "End"
                        }
                    ]
                }
                """;

        // When & Then
        BookValidationException exception = assertThrows(BookValidationException.class,
                () -> bookService.createBook(invalidJson));

        assertTrue(exception.getMessage().contains("gotoId") || exception.getMessage().contains("invalid"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle books with array tags in JSON")
    void testCreateBook_ArrayTagsInJson() {
        // Given
        String bookJsonWithArrayTags = """
                {
                    "title": "Book With Array Tags",
                    "author": "Author",
                    "difficulty": "HARD",
                    "tags": ["fantasy", "adventure", "mystery"],
                    "sections": [
                        {
                            "id": 1,
                            "type": "BEGIN",
                            "text": "Start",
                            "options": [{"text": "Continue", "gotoId": 2}]
                        },
                        {
                            "id": 2,
                            "type": "END",
                            "text": "End"
                        }
                    ]
                }
                """;

        Book savedBook = Book.builder()
                .id(1L)
                .title("Book With Array Tags")
                .author("Author")
                .difficulty("HARD")
                .tags("fantasy,adventure,mystery")
                .jsonContent(bookJsonWithArrayTags)
                .build();

        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

        // When
        BookDTO result = bookService.createBook(bookJsonWithArrayTags);

        // Then
        assertNotNull(result.getTags());
        assertEquals(3, result.getTags().size());
    }
}
