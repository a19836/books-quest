package com.booksquest.internal.manage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.booksquest.internal.manage",
    "com.booksquest.shared"
})
@EntityScan(basePackages = {"com.booksquest.internal.manage.entity", "com.booksquest.shared.entity"})
@EnableJpaRepositories(basePackages = "com.booksquest.internal.manage.repository")
public class ManageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ManageServiceApplication.class, args);
    }
}
