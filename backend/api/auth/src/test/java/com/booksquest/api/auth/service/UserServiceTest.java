package com.booksquest.api.auth.service;

import com.booksquest.api.auth.entity.User;
import com.booksquest.api.auth.exception.AuthException;
import com.booksquest.api.auth.exception.ValidationException;
import com.booksquest.api.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "ValidPassword123";
    private static final String HASHED_PASSWORD = "hashedPassword";

    @BeforeEach
    void setUp() {
        // Don't setup password encoder mocks in setUp
        // Setup them per-test only when needed
    }

    // ===== CREATE USER TESTS =====

    @Test
    @DisplayName("Should successfully create user with valid data")
    void testCreateUser_Success() {
        // Given
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);

        User expectedUser = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .username(TEST_USERNAME)
                .passwordHash(HASHED_PASSWORD)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        // When
        User result = userService.createUser(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD);

        // Then
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USERNAME, result.getUsername());
        assertEquals(HASHED_PASSWORD, result.getPasswordHash());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testCreateUser_DuplicateEmail() {
        // Given
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        // When & Then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.createUser(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD));

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when password is too short")
    void testCreateUser_ShortPassword() {
        // Given
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(TEST_EMAIL, TEST_USERNAME, "Short1"));

        assertTrue(exception.getMessage().contains("at least 8 characters"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when password has no letters")
    void testCreateUser_NoLettersInPassword() {
        // Given
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(TEST_EMAIL, TEST_USERNAME, "12345678"));

        assertTrue(exception.getMessage().contains("letter and 1 number"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when password has no numbers")
    void testCreateUser_NoNumbersInPassword() {
        // Given
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(TEST_EMAIL, TEST_USERNAME, "OnlyLetters"));

        assertTrue(exception.getMessage().contains("letter and 1 number"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when password is null")
    void testCreateUser_NullPassword() {
        // Given
        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(TEST_EMAIL, TEST_USERNAME, null));

        assertTrue(exception.getMessage().contains("at least 8 characters"));
        verify(userRepository, never()).save(any(User.class));
    }

    // ===== FIND BY EMAIL TESTS =====

    @Test
    @DisplayName("Should find user by email successfully")
    void testFindByEmail_Success() {
        // Given
        User expectedUser = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .username(TEST_USERNAME)
                .passwordHash(HASHED_PASSWORD)
                .build();

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(expectedUser));

        // When
        User result = userService.findByEmail(TEST_EMAIL);

        // Then
        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.getEmail());
        assertEquals(TEST_USERNAME, result.getUsername());
    }

    @Test
    @DisplayName("Should throw exception when user not found by email")
    void testFindByEmail_NotFound() {
        // Given
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // When & Then
        AuthException exception = assertThrows(AuthException.class,
                () -> userService.findByEmail(TEST_EMAIL));

        assertEquals("User not found", exception.getMessage());
    }

    // ===== FIND BY ID TESTS =====

    @Test
    @DisplayName("Should find user by ID successfully")
    void testFindById_Success() {
        // Given
        long userId = 1L;
        User expectedUser = User.builder()
                .id(userId)
                .email(TEST_EMAIL)
                .username(TEST_USERNAME)
                .passwordHash(HASHED_PASSWORD)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

        // When
        User result = userService.findById(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(TEST_EMAIL, result.getEmail());
    }

    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void testFindById_NotFound() {
        // Given
        long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        AuthException exception = assertThrows(AuthException.class,
                () -> userService.findById(userId));

        assertEquals("User not found", exception.getMessage());
    }

    // ===== VERIFY PASSWORD TESTS =====

    @Test
    @DisplayName("Should verify correct password")
    void testVerifyPassword_Correct() {
        // Given
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // When
        boolean result = userService.verifyPassword(TEST_PASSWORD, HASHED_PASSWORD);

        // Then
        assertTrue(result);
        verify(passwordEncoder).matches(TEST_PASSWORD, HASHED_PASSWORD);
    }

    @Test
    @DisplayName("Should return false for incorrect password")
    void testVerifyPassword_Incorrect() {
        // Given
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When
        boolean result = userService.verifyPassword("WrongPassword", HASHED_PASSWORD);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle empty password verification")
    void testVerifyPassword_Empty() {
        // Given
        when(passwordEncoder.matches("", HASHED_PASSWORD)).thenReturn(false);

        // When
        boolean result = userService.verifyPassword("", HASHED_PASSWORD);

        // Then
        assertFalse(result);
    }
}
