package com.booksquest.api.auth.service;

import com.booksquest.api.auth.dto.AuthResponse;
import com.booksquest.api.auth.dto.LoginRequest;
import com.booksquest.api.auth.dto.RegisterRequest;
import com.booksquest.api.auth.entity.User;
import com.booksquest.api.auth.exception.AuthException;
import com.booksquest.shared.util.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "ValidPassword123";
    private static final String TEST_TOKEN = "jwt.token.here";
    private static final long EXPIRATION_MS = 3600000L;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email(TEST_EMAIL)
                .username(TEST_USERNAME)
                .passwordHash("hashedPassword")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ===== REGISTER TESTS =====

    @Test
    @DisplayName("Should successfully register user")
    void testRegister_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(TEST_EMAIL);
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);

        when(userService.createUser(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD)).thenReturn(testUser);
        when(jwtProvider.generateToken(1L, TEST_EMAIL)).thenReturn(TEST_TOKEN);
        when(jwtProvider.getExpirationTimeInMillis()).thenReturn(EXPIRATION_MS);

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals(TEST_USERNAME, response.getUsername());
        assertEquals(EXPIRATION_MS, response.getExpiresIn());

        verify(userService).createUser(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD);
        verify(jwtProvider).generateToken(1L, TEST_EMAIL);
    }

    @Test
    @DisplayName("Should throw exception when registration fails due to duplicate email")
    void testRegister_DuplicateEmail() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(TEST_EMAIL);
        request.setUsername(TEST_USERNAME);
        request.setPassword(TEST_PASSWORD);

        when(userService.createUser(TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD))
                .thenThrow(new AuthException("Email already registered"));

        // When & Then
        AuthException exception = assertThrows(AuthException.class,
                () -> authService.register(request));

        assertEquals("Email already registered", exception.getMessage());
        verify(jwtProvider, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when registration fails due to weak password")
    void testRegister_WeakPassword() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail(TEST_EMAIL);
        request.setUsername(TEST_USERNAME);
        request.setPassword("weak");

        when(userService.createUser(TEST_EMAIL, TEST_USERNAME, "weak"))
                .thenThrow(new IllegalArgumentException("Password must be at least 8 characters long"));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));

        assertTrue(exception.getMessage().contains("8 characters"));
        verify(jwtProvider, never()).generateToken(anyLong(), anyString());
    }

    // ===== LOGIN TESTS =====

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLogin_Success() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(userService.verifyPassword(TEST_PASSWORD, testUser.getPasswordHash())).thenReturn(true);
        when(jwtProvider.generateToken(1L, TEST_EMAIL)).thenReturn(TEST_TOKEN);
        when(jwtProvider.getExpirationTimeInMillis()).thenReturn(EXPIRATION_MS);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals(TEST_USERNAME, response.getUsername());

        verify(userService).findByEmail(TEST_EMAIL);
        verify(userService).verifyPassword(TEST_PASSWORD, testUser.getPasswordHash());
        verify(jwtProvider).generateToken(1L, TEST_EMAIL);
    }

    @Test
    @DisplayName("Should throw exception when login with incorrect password")
    void testLogin_InvalidPassword() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(TEST_EMAIL);
        request.setPassword("WrongPassword123");

        when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(userService.verifyPassword("WrongPassword123", testUser.getPasswordHash())).thenReturn(false);

        // When & Then
        AuthException exception = assertThrows(AuthException.class,
                () -> authService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtProvider, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should throw exception when login with non-existent email")
    void testLogin_UserNotFound() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword(TEST_PASSWORD);

        when(userService.findByEmail("nonexistent@example.com"))
                .thenThrow(new AuthException("User not found"));

        // When & Then
        AuthException exception = assertThrows(AuthException.class,
                () -> authService.login(request));

        assertEquals("User not found", exception.getMessage());
        verify(jwtProvider, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should generate correct token after login")
    void testLogin_TokenGeneration() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(userService.verifyPassword(TEST_PASSWORD, testUser.getPasswordHash())).thenReturn(true);
        when(jwtProvider.generateToken(1L, TEST_EMAIL)).thenReturn(TEST_TOKEN);
        when(jwtProvider.getExpirationTimeInMillis()).thenReturn(EXPIRATION_MS);

        // When
        AuthResponse response = authService.login(request);

        // Then
        verify(jwtProvider).generateToken(1L, TEST_EMAIL);
        assertEquals(TEST_TOKEN, response.getToken());
    }

    @Test
    @DisplayName("Should return all user details in auth response")
    void testLogin_ResponseContainsAllDetails() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        when(userService.findByEmail(TEST_EMAIL)).thenReturn(testUser);
        when(userService.verifyPassword(TEST_PASSWORD, testUser.getPasswordHash())).thenReturn(true);
        when(jwtProvider.generateToken(1L, TEST_EMAIL)).thenReturn(TEST_TOKEN);
        when(jwtProvider.getExpirationTimeInMillis()).thenReturn(EXPIRATION_MS);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response.getToken());
        assertNotNull(response.getUserId());
        assertNotNull(response.getEmail());
        assertNotNull(response.getUsername());
        assertNotNull(response.getExpiresIn());
    }
}
