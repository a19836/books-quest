package com.booksquest.internal.manage.service;

import com.booksquest.shared.entity.Book;
import com.booksquest.internal.manage.exception.BookValidationException;
import com.booksquest.internal.manage.repository.BookRepository;
import com.booksquest.internal.manage.util.BookValidator;
import com.booksquest.shared.dto.BookDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("manageBookService")
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final ObjectMapper objectMapper;

    public BookDTO createBook(String jsonContent) {
        try {
            JsonNode bookJson = objectMapper.readTree(jsonContent);
            BookValidator.validateBook(bookJson);

            List<String> tags = parseTags(bookJson);

            Book book = Book.builder()
                .title(bookJson.get("title").asText())
                .author(bookJson.get("author").asText())
                .difficulty(bookJson.get("difficulty").asText())
                .category(bookJson.has("category") ? bookJson.get("category").asText() : null)
                .description(bookJson.has("description") ? bookJson.get("description").asText() : null)
                .tags(tags.isEmpty() ? null : String.join(",", tags))
                .fastReadingMinutes(bookJson.has("fastReadingMinutes") ? bookJson.get("fastReadingMinutes").asInt() : null)
                .slowReadingMinutes(bookJson.has("slowReadingMinutes") ? bookJson.get("slowReadingMinutes").asInt() : null)
                .jsonContent(jsonContent)
                .build();

            Book savedBook = bookRepository.save(book);
            return convertToDTO(savedBook);
        } catch (Exception e) {
            if (e instanceof BookValidationException) {
                throw (BookValidationException) e;
            }
            throw new RuntimeException("Error creating book: " + e.getMessage(), e);
        }
    }

    private List<String> parseTags(JsonNode bookJson) {
        List<String> tags = new ArrayList<>();
        if (bookJson.has("tags")) {
            JsonNode tagsNode = bookJson.get("tags");
            if (tagsNode.isArray()) {
                tagsNode.forEach(tag -> tags.add(tag.asText()));
            }
        }
        return tags;
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
