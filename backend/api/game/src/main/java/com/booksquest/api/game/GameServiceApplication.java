package com.booksquest.api.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.booksquest.api.game",
    "com.booksquest.shared"
})
@EntityScan(basePackages = {"com.booksquest.api.game.entity", "com.booksquest.shared.entity"})
@EnableJpaRepositories(basePackages = "com.booksquest.api.game.repository")
public class GameServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}
