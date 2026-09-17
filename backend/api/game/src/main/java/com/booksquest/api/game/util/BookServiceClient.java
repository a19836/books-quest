package com.booksquest.api.game.util;

import com.booksquest.shared.dto.BookDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookServiceClient {
    private final RestTemplate restTemplate;

    @Value("${book.service.url:http://localhost:9090/api/books}")
    private String bookServiceUrl;

    public BookDTO getBookById(Long bookId) {
        try {
            String url = bookServiceUrl + "/" + bookId;
            BookDTO bookDTO = restTemplate.getForObject(url, BookDTO.class);
            if (bookDTO == null) {
                throw new RuntimeException("Book not found with id: " + bookId);
            }
            return bookDTO;
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Book not found with id: " + bookId, e);
        } catch (Exception e) {
            log.error("Error fetching book from Book Service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch book from Book Service: " + e.getMessage(), e);
        }
    }
}
