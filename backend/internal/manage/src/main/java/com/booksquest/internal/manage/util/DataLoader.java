package com.booksquest.internal.manage.util;

import com.booksquest.internal.manage.repository.BookRepository;
import com.booksquest.internal.manage.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final BookService bookService;
    private final BookRepository bookRepository;

    @Value("${app.books.path:./books}")
    private String booksPath;

    @Override
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            return;
        }

        loadBooksFromPath(booksPath);
    }

    private void loadBooksFromPath(String dirPath) {
        try (Stream<Path> paths = Files.list(Paths.get(dirPath))) {
            paths.filter(path -> path.toString().endsWith(".json"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        if (!content.trim().isEmpty() && !content.trim().equals("{}")) {
                            bookService.createBook(content);
                            System.out.println("Loaded book from: " + path.getFileName());
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to load book from " + path + ": " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            System.err.println("Failed to load books: " + e.getMessage());
        }
    }
}

