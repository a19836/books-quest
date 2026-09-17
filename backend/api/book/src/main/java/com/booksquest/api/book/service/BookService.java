package com.booksquest.api.book.service;

import com.booksquest.shared.entity.Book;
import com.booksquest.api.book.exception.BookNotFoundException;
import com.booksquest.api.book.repository.BookRepository;
import com.booksquest.shared.dto.BookDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("bookReadService")
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper;

    public List<BookDTO> getAllBooks() {
        return bookRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Deprecated
    public List<BookDTO> searchBooks(String keyword) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(keyword, keyword)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<BookDTO> searchAndFilterBooks(String search, String difficulty, String category, String tags) {
        List<Book> results = bookRepository.findAll();

        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            results = results.stream()
                .filter(book ->
                    (book.getTitle() != null && book.getTitle().toLowerCase().contains(searchLower)) ||
                    (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(searchLower)) ||
                    (book.getDescription() != null && book.getDescription().toLowerCase().contains(searchLower)) ||
                    (book.getCategory() != null && book.getCategory().toLowerCase().contains(searchLower)) ||
                    (book.getTags() != null && book.getTags().toLowerCase().contains(searchLower))
                )
                .collect(Collectors.toList());
        }

        if (difficulty != null && !difficulty.trim().isEmpty()) {
            results = results.stream()
                .filter(book -> book.getDifficulty().equalsIgnoreCase(difficulty))
                .collect(Collectors.toList());
        }

        if (category != null && !category.trim().isEmpty()) {
            results = results.stream()
                .filter(book -> book.getCategory() != null && book.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
        }

        if (tags != null && !tags.trim().isEmpty()) {
            String[] tagArray = tags.split(",");
            results = results.stream()
                .filter(book -> book.getTags() != null &&
                    java.util.Arrays.stream(tagArray).anyMatch(tag ->
                        book.getTags().toLowerCase().contains(tag.trim().toLowerCase())
                    )
                )
                .collect(Collectors.toList());
        }

        return results.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public BookDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
            .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
        return convertToDTO(book);
    }

    private BookDTO convertToDTO(Book book) {
        try {
            JsonNode content = objectMapper.readTree(book.getJsonContent());
            List<String> tags = new ArrayList<>();
            if (book.getTags() != null && !book.getTags().isEmpty()) {
                tags = List.of(book.getTags().split(","));
            }

            Integer chapters = 0;
            if (content.has("sections") && content.get("sections").isArray()) {
                chapters = content.get("sections").size();
            }

            return BookDTO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .difficulty(book.getDifficulty())
                .category(book.getCategory())
                .description(book.getDescription())
                .tags(tags)
                .fastReadingMinutes(book.getFastReadingMinutes())
                .slowReadingMinutes(book.getSlowReadingMinutes())
                .chapters(chapters)
                .content(content)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Error converting book to DTO: " + e.getMessage(), e);
        }
    }
}
