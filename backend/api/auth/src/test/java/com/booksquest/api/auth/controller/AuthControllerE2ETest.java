package com.booksquest.api.auth.controller;

import com.booksquest.api.auth.dto.LoginRequest;
import com.booksquest.api.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("AuthController E2E Tests")
class AuthControllerE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "http://localhost";
    private static final String AUTH_API = "/api/auth";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = AUTH_API;
    }

    // ===== REGISTER ENDPOINT TESTS =====

    @Test
    @DisplayName("Should successfully register new user")
    void testRegister_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setUsername("newuser");
        request.setPassword("ValidPassword123");

        // When
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/register");

        // Then
        response.then()
                .statusCode(201)
                .body("token", notNullValue())
                .body("userId", notNullValue())
                .body("email", equalTo("newuser@example.com"))
                .body("username", equalTo("newuser"))
                .body("expiresIn", notNullValue());
    }

    @Test
    @DisplayName("Should return 400 when registering with duplicate email")
    void testRegister_DuplicateEmail() {
        // Given - Register first user
        RegisterRequest request1 = new RegisterRequest();
        request1.setEmail("duplicate@example.com");
        request1.setUsername("user1");
        request1.setPassword("ValidPassword123");

        given()
                .contentType(ContentType.JSON)
                .body(request1)
                .when()
                .post("/register")
                .then()
                .statusCode(201);

        // When - Try to register with same email
        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("duplicate@example.com");
        request2.setUsername("user2");
        request2.setPassword("AnotherValid123");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(request2)
                .when()
                .post("/register");

        // Then
        response.then()
                .statusCode(400)
                .body("message", containsString("already registered"));
    }

    @Test
    @DisplayName("Should return 400 when registering with short password")
    void testRegister_ShortPassword() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setUsername("testuser");
        request.setPassword("Short1");

        // When
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/register");

        // Then
        response.then()
                .statusCode(400)
                .body("message", containsString("8 characters"));
    }

    @Test
    @DisplayName("Should return 400 when registering with password lacking letters")
    void testRegister_NoLettersInPassword() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setUsername("testuser");
        request.setPassword("12345678");

        // When
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/register");

        // Then
        response.then()
                .statusCode(400)
                .body("message", containsString("letter"));
    }

    @Test
    @DisplayName("Should return 400 when registering with password lacking numbers")
    void testRegister_NoNumbersInPassword() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setUsername("testuser");
        request.setPassword("NoNumbers");

        // When
        Response response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/register");

        // Then
        response.then()
                .statusCode(400)
                .body("message", containsString("number"));
    }

    @Test
    @DisplayName("Should return token that can be used for subsequent requests")
    void testRegister_ReturnsValidToken() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("tokentest@example.com");
        request.setUsername("tokenuser");
        request.setPassword("ValidPassword123");

        // When
        Response registerResponse = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/register");

        String token = registerResponse.then()
                .statusCode(201)
                .extract()
                .path("token");

        // Then
        assertTokenIsValid(token);
    }

    // ===== LOGIN ENDPOINT TESTS =====

    @Test
    @DisplayName("Should successfully login with valid credentials")
    void testLogin_Success() {
        // Given - Register user first
        registerTestUser("login@example.com", "loginuser", "ValidPassword123");

        // When
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("ValidPassword123");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/login");

        // Then
        response.then()
                .statusCode(200)
                .body("token", notNullValue())
                .body("userId", notNullValue())
                .body("email", equalTo("login@example.com"))
                .body("username", equalTo("loginuser"));
    }

    @Test
    @DisplayName("Should return 401 when login with incorrect password")
    void testLogin_InvalidPassword() {
        // Given - Register user first
        registerTestUser("wrongpass@example.com", "wronguser", "ValidPassword123");

        // When
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrongpass@example.com");
        loginRequest.setPassword("WrongPassword123");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/login");

        // Then
        response.then()
                .statusCode(401)
                .body("message", containsString("Invalid email or password"));
    }

    @Test
    @DisplayName("Should return 401 when login with non-existent email")
    void testLogin_UserNotFound() {
        // When
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("AnyPassword123");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/login");

        // Then
        response.then()
                .statusCode(401)
                .body("message", containsString("not found"));
    }

    @Test
    @DisplayName("Should return 400 for malformed login request")
    void testLogin_MalformedRequest() {
        // When
        Response response = given()
                .contentType(ContentType.JSON)
                .body("{invalid json")
                .when()
                .post("/login");

        // Then
        response.then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Should return different tokens for same user on multiple logins")
    void testLogin_DifferentTokensPerLogin() {
        // Given
        registerTestUser("multilogin@example.com", "multiuser", "ValidPassword123");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("multilogin@example.com");
        loginRequest.setPassword("ValidPassword123");

        // When
        String token1 = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        String token2 = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");

        // Then - Tokens should be different (issued at different times)
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Should return correct user info on login")
    void testLogin_ReturnsCorrectUserInfo() {
        // Given
        registerTestUser("userinfo@example.com", "infouser", "ValidPassword123");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("userinfo@example.com");
        loginRequest.setPassword("ValidPassword123");

        // When
        Response response = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/login");

        // Then
        response.then()
                .statusCode(200)
                .body("email", equalTo("userinfo@example.com"))
                .body("username", equalTo("infouser"))
                .body("userId", notNullValue())
                .body("expiresIn", notNullValue());
    }

    // ===== HELPER METHODS =====

    private void registerTestUser(String email, String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setUsername(username);
        request.setPassword(password);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/register")
                .then()
                .statusCode(201);
    }

    private void assertTokenIsValid(String token) {
        // Token should not be null or empty
        assert token != null;
        assert !token.isEmpty();
        // JWT tokens have 3 parts separated by dots
        assert token.split("\\.").length == 3;
    }
}
