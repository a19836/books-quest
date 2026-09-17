package com.booksquest.api.book.service;

import com.booksquest.api.book.exception.BookNotFoundException;
import com.booksquest.api.book.repository.BookRepository;
import com.booksquest.shared.entity.Book;
import com.booksquest.shared.dto.BookDTO;
import com.booksquest.shared.testdata.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService Unit Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ObjectMapper mockObjectMapper;

    @InjectMocks
    private BookService bookService;

    private Book testBook;
    private BookDTO testBookDTO;
    private ObjectMapper realObjectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id(1L)
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

        testBookDTO = TestDataBuilder.buildTestBook();
    }

    private void setupObjectMapperMock() throws Exception {
        // Setup ObjectMapper mock to handle JSON parsing
        /*
        What it does:
        • The mock mockObjectMapper is intercepted
        • When readTree() is called, instead of returning a mock value, it delegates to the real ObjectMapper
        • inv.getArgument(0) gets the JSON string that was passed in
        • realObjectMapper.readTree() actually parses it

        Why this approach:
        • Without this, readTree() would return a mock that doesn't actually parse JSON. This setup makes the mocked ObjectMapper behave like a real one for this specific method, while still mocking the rest of the service.
        • It's a hybrid approach: mock the service dependencies but use real JSON parsing so tests can focus on business logic, not JSON parsing.
        */
        when(mockObjectMapper.readTree((String) any()))
                .thenAnswer(inv -> realObjectMapper.readTree((String) inv.getArgument(0)));
    }

    // ===== GET ALL BOOKS TESTS =====

    @Test
    @DisplayName("Should return all books successfully")
    void testGetAllBooks_Success() throws Exception {
        // Given
        setupObjectMapperMock();
        Book book1 = Book.builder()
                .id(1L)
                .title("Book 1")
                .author("Author 1")
                .difficulty("EASY")
                .category("Fantasy")
                .description("Description 1")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        Book book2 = Book.builder()
                .id(2L)
                .title("Book 2")
                .author("Author 2")
                .difficulty("HARD")
                .category("Mystery")
                .description("Description 2")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));

        // When
        List<BookDTO> result = bookService.getAllBooks();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Book 1", result.get(0).getTitle());
        assertEquals("Book 2", result.get(1).getTitle());
        verify(bookRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no books exist")
    void testGetAllBooks_Empty() {
        // Given
        when(bookRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<BookDTO> result = bookService.getAllBooks();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ===== GET BOOK BY ID TESTS =====

    @Test
    @DisplayName("Should return book by ID successfully")
    void testGetBookById_Success() throws Exception {
        // Given
        setupObjectMapperMock();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // When
        BookDTO result = bookService.getBookById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Adventure", result.getTitle());
        assertEquals("Test Author", result.getAuthor());
        verify(bookRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when book not found")
    void testGetBookById_NotFound() {
        // Given
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        BookNotFoundException exception = assertThrows(BookNotFoundException.class,
                () -> bookService.getBookById(999L));

        assertTrue(exception.getMessage().contains("not found"));
        verify(bookRepository).findById(999L);
    }

    @Test
    @DisplayName("Should parse tags correctly from book")
    void testGetBookById_ParsesTags() throws Exception {
        // Given
        setupObjectMapperMock();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // When
        BookDTO result = bookService.getBookById(1L);

        // Then
        assertNotNull(result.getTags());
        assertTrue(result.getTags().contains("fantasy"));
        assertTrue(result.getTags().contains("adventure"));
    }

    @Test
    @DisplayName("Should count chapters correctly from book content")
    void testGetBookById_CountsChapters() throws Exception {
        // Given
        setupObjectMapperMock();
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        // When
        BookDTO result = bookService.getBookById(1L);

        // Then
        assertNotNull(result.getChapters());
        assertEquals(5, result.getChapters());
    }

    // ===== SEARCH AND FILTER TESTS =====

    @Test
    @DisplayName("Should search books by keyword")
    void testSearchAndFilterBooks_SearchByKeyword() throws Exception {
        // Given
        setupObjectMapperMock();
        Book book1 = Book.builder()
                .id(1L)
                .title("Fantasy Quest")
                .author("Author")
                .difficulty("EASY")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        Book book2 = Book.builder()
                .id(2L)
                .title("Mystery Novel")
                .author("Author")
                .difficulty("HARD")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));

        // When
        List<BookDTO> result = bookService.searchAndFilterBooks("Fantasy", null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Fantasy Quest", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Should filter books by difficulty")
    void testSearchAndFilterBooks_FilterByDifficulty() throws Exception {
        // Given
        setupObjectMapperMock();
        Book book1 = Book.builder()
                .id(1L)
                .title("Book 1")
                .author("Author")
                .difficulty("EASY")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        Book book2 = Book.builder()
                .id(2L)
                .title("Book 2")
                .author("Author")
                .difficulty("HARD")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));

        // When
        List<BookDTO> result = bookService.searchAndFilterBooks(null, "EASY", null, null);

        // Then
        assertEquals(1, result.size());
        assertEquals("EASY", result.get(0).getDifficulty());
    }

    @Test
    @DisplayName("Should filter books by category")
    void testSearchAndFilterBooks_FilterByCategory() throws Exception {
        // Given
        setupObjectMapperMock();
        Book book1 = Book.builder()
                .id(1L)
                .title("Book 1")
                .author("Author")
                .category("Fantasy")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        Book book2 = Book.builder()
                .id(2L)
                .title("Book 2")
                .author("Author")
                .category("Mystery")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));

        // When
        List<BookDTO> result = bookService.searchAndFilterBooks(null, null, "Fantasy", null);

        // Then
        assertEquals(1, result.size());
        assertEquals("Fantasy", result.get(0).getCategory());
    }

    @Test
    @DisplayName("Should filter books by tags")
    void testSearchAndFilterBooks_FilterByTags() throws Exception {
        // Given
        setupObjectMapperMock();
        Book book1 = Book.builder()
                .id(1L)
                .title("Book 1")
                .author("Author")
                .tags("fantasy,adventure")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        Book book2 = Book.builder()
                .id(2L)
                .title("Book 2")
                .author("Author")
                .tags("mystery,detective")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));

        // When
        List<BookDTO> result = bookService.searchAndFilterBooks(null, null, null, "fantasy");

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getTags().contains("fantasy"));
    }

    @Test
    @DisplayName("Should apply multiple filters together")
    void testSearchAndFilterBooks_MultipleFilters() throws Exception {
        // Given
        setupObjectMapperMock();
        Book book1 = Book.builder()
                .id(1L)
                .title("Fantasy Quest")
                .author("Author")
                .difficulty("EASY")
                .category("Fantasy")
                .tags("fantasy,adventure")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        Book book2 = Book.builder()
                .id(2L)
                .title("Mystery Novel")
                .author("Author")
                .difficulty("HARD")
                .category("Mystery")
                .tags("mystery,detective")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));

        // When
        List<BookDTO> result = bookService.searchAndFilterBooks("Fantasy", "EASY", "Fantasy", "fantasy");

        // Then
        assertEquals(1, result.size());
        assertEquals("Fantasy Quest", result.get(0).getTitle());
        assertEquals("EASY", result.get(0).getDifficulty());
    }

    @Test
    @DisplayName("Should return empty list when no books match filters")
    void testSearchAndFilterBooks_NoMatches() {
        // Given
        when(bookRepository.findAll()).thenReturn(Arrays.asList());

        // When
        List<BookDTO> result = bookService.searchAndFilterBooks("NonExistent", null, null, null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle case-insensitive search")
    void testSearchAndFilterBooks_CaseInsensitive() throws Exception {
        // Given
        setupObjectMapperMock();
        Book book = Book.builder()
                .id(1L)
                .title("Fantasy Quest")
                .author("Author")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        when(bookRepository.findAll()).thenReturn(Arrays.asList(book));

        // When
        List<BookDTO> result = bookService.searchAndFilterBooks("fantasy", null, null, null);

        // Then
        assertEquals(1, result.size());
        assertEquals("Fantasy Quest", result.get(0).getTitle());
    }

    @Test
    @DisplayName("Should filter by multiple tags")
    void testSearchAndFilterBooks_MultipleTagsFilter() throws Exception {
        // Given
        setupObjectMapperMock();
        Book book1 = Book.builder()
                .id(1L)
                .title("Book 1")
                .author("Author")
                .tags("fantasy,adventure,quest")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        Book book2 = Book.builder()
                .id(2L)
                .title("Book 2")
                .author("Author")
                .tags("mystery,detective")
                .jsonContent(TestDataBuilder.buildTestBookJsonString())
                .build();

        when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));

        // When
        List<BookDTO> result = bookService.searchAndFilterBooks(null, null, null, "adventure,quest");

        // Then
        assertEquals(1, result.size());
    }
}
