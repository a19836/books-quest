package com.booksquest.api.auth.service;

import com.booksquest.api.auth.entity.User;
import com.booksquest.api.auth.exception.AuthException;
import com.booksquest.api.auth.exception.ValidationException;
import com.booksquest.api.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(String email, String username, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email already registered");
        }

        validatePassword(rawPassword);

        String hashedPassword = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash(hashedPassword)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found"));
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
    }

    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasNumber = password.matches(".*\\d.*");

        if (!hasLetter || !hasNumber) {
            throw new IllegalArgumentException("Password must contain at least 1 letter and 1 number");
        }
    }
}
