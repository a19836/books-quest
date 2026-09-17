package com.booksquest.api.auth.jwt;

import com.booksquest.shared.util.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtProvider Unit Tests")
class JwtProviderTest {

    private JwtProvider jwtProvider;
    // Generate a proper 512-bit (64 bytes) key for HS512
    private static final String SECRET = "32844c57fc247951b7070489871810bf6b97cc35691c7bdea5801d0cb9df757bc6896321442bac81e2ab78b3a0727e7081a89c6909b7e2ba611163a3bec3ba36";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtProvider, "jwtExpirationMs", EXPIRATION_MS);
    }

    // ===== GENERATE TOKEN TESTS =====

    @Test
    @DisplayName("Should generate valid token with user ID and email")
    void testGenerateToken_Success() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";

        // When
        String token = jwtProvider.generateToken(userId, email);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    @DisplayName("Should generate token with correct user ID claim")
    void testGenerateToken_CorrectUserId() {
        // Given
        Long userId = 123L;
        String email = "test@example.com";

        // When
        String token = jwtProvider.generateToken(userId, email);

        // Then
        Long extractedUserId = jwtProvider.getUserIdFromToken(token);
        assertEquals(userId, extractedUserId);
    }

    @Test
    @DisplayName("Should generate token with correct email claim")
    void testGenerateToken_CorrectEmail() {
        // Given
        Long userId = 1L;
        String email = "test@example.com";

        // When
        String token = jwtProvider.generateToken(userId, email);

        // Then
        String extractedEmail = jwtProvider.getEmailFromToken(token);
        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void testGenerateToken_DifferentUsers() {
        // When
        String token1 = jwtProvider.generateToken(1L, "user1@example.com");
        String token2 = jwtProvider.generateToken(2L, "user2@example.com");

        // Then
        assertNotEquals(token1, token2);
    }

    // ===== GET USER ID FROM TOKEN TESTS =====

    @Test
    @DisplayName("Should extract correct user ID from token")
    void testGetUserIdFromToken_Success() {
        // Given
        Long userId = 42L;
        String token = jwtProvider.generateToken(userId, "test@example.com");

        // When
        Long extractedUserId = jwtProvider.getUserIdFromToken(token);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    @DisplayName("Should throw exception for invalid token format")
    void testGetUserIdFromToken_InvalidToken() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThrows(Exception.class, () -> jwtProvider.getUserIdFromToken(invalidToken));
    }

    @Test
    @DisplayName("Should throw exception for tampered token")
    void testGetUserIdFromToken_TamperedToken() {
        // Given
        String validToken = jwtProvider.generateToken(1L, "test@example.com");
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "xxxxx";

        // When & Then
        assertThrows(Exception.class, () -> jwtProvider.getUserIdFromToken(tamperedToken));
    }

    // ===== GET EMAIL FROM TOKEN TESTS =====

    @Test
    @DisplayName("Should extract correct email from token")
    void testGetEmailFromToken_Success() {
        // Given
        String email = "user@example.com";
        String token = jwtProvider.generateToken(1L, email);

        // When
        String extractedEmail = jwtProvider.getEmailFromToken(token);

        // Then
        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("Should throw exception for invalid token")
    void testGetEmailFromToken_InvalidToken() {
        // Given
        String invalidToken = "not.a.token";

        // When & Then
        assertThrows(Exception.class, () -> jwtProvider.getEmailFromToken(invalidToken));
    }

    // ===== VALIDATE TOKEN TESTS =====

    @Test
    @DisplayName("Should validate correct token successfully")
    void testValidateToken_Valid() {
        // Given
        String token = jwtProvider.generateToken(1L, "test@example.com");

        // When
        boolean isValid = jwtProvider.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should return false for invalid token format")
    void testValidateToken_InvalidFormat() {
        // Given
        String invalidToken = "invalid.token.format";

        // When
        boolean isValid = jwtProvider.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for tampered token")
    void testValidateToken_Tampered() {
        // Given
        String validToken = jwtProvider.generateToken(1L, "test@example.com");
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "xxxxx";

        // When
        boolean isValid = jwtProvider.validateToken(tamperedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for null token")
    void testValidateToken_Null() {
        // When
        boolean isValid = jwtProvider.validateToken(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should return false for empty token")
    void testValidateToken_Empty() {
        // When
        boolean isValid = jwtProvider.validateToken("");

        // Then
        assertFalse(isValid);
    }

    // ===== EXPIRATION TESTS =====

    @Test
    @DisplayName("Should return correct expiration time in milliseconds")
    void testGetExpirationTimeInMillis() {
        // When
        long expirationMs = jwtProvider.getExpirationTimeInMillis();

        // Then
        assertEquals(EXPIRATION_MS, expirationMs);
    }

    @Test
    @DisplayName("Should handle special characters in email")
    void testGenerateToken_SpecialCharactersInEmail() {
        // Given
        String email = "test+special@example.co.uk";
        Long userId = 1L;

        // When
        String token = jwtProvider.generateToken(userId, email);
        String extractedEmail = jwtProvider.getEmailFromToken(token);

        // Then
        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("Should generate token with same claims consistently")
    void testGenerateToken_Consistency() {
        // Given
        Long userId = 100L;
        String email = "consistent@example.com";

        // When
        String token1 = jwtProvider.generateToken(userId, email);
        Long extractedUserId1 = jwtProvider.getUserIdFromToken(token1);
        String extractedEmail1 = jwtProvider.getEmailFromToken(token1);

        // Then
        assertEquals(userId, extractedUserId1);
        assertEquals(email, extractedEmail1);
        assertTrue(jwtProvider.validateToken(token1));
    }
}
