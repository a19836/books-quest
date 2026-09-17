package com.booksquest.api.book;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.booksquest.api.book",
    "com.booksquest.shared"
})
@EntityScan(basePackages = {"com.booksquest.api.book.entity", "com.booksquest.shared.entity"})
@EnableJpaRepositories(basePackages = "com.booksquest.api.book.repository")
public class BookServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookServiceApplication.class, args);
    }
}
