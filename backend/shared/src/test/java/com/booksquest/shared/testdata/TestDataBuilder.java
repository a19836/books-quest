package com.booksquest.shared.testdata;

import com.booksquest.shared.dto.BookDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

public class TestDataBuilder {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static BookDTO buildTestBook() {
        return BookDTO.builder()
                .id(1L)
                .title("Test Adventure")
                .author("Test Author")
                .difficulty("MEDIUM")
                .category("Fantasy")
                .description("A test adventure book")
                .tags(Arrays.asList("fantasy", "adventure"))
                .fastReadingMinutes(30)
                .slowReadingMinutes(60)
                .chapters(5)
                .content(buildTestBookContent())
                .build();
    }

    public static BookDTO buildTestBook(Long id, String title, String difficulty) {
        return BookDTO.builder()
                .id(id)
                .title(title)
                .author("Test Author")
                .difficulty(difficulty)
                .category("Fantasy")
                .description("A test adventure book")
                .tags(Arrays.asList("fantasy"))
                .fastReadingMinutes(30)
                .slowReadingMinutes(60)
                .chapters(5)
                .content(buildTestBookContent())
                .build();
    }

    public static JsonNode buildTestBookContent() {
        try {
            String json = """
                    {
                        "id": 1,
                        "title": "Test Adventure",
                        "author": "Test Author",
                        "difficulty": "MEDIUM",
                        "category": "Fantasy",
                        "sections": [
                            {
                                "id": 1,
                                "type": "BEGIN",
                                "text": "Welcome to the adventure!",
                                "options": [
                                    {"text": "Go left", "gotoId": 2}
                                ]
                            },
                            {
                                "id": 2,
                                "type": "NORMAL",
                                "text": "You are in a forest.",
                                "options": [
                                    {
                                        "text": "Fight the dragon",
                                        "gotoId": 3,
                                        "consequence": {
                                            "type": "LOSE_HEALTH",
                                            "value": 3
                                        }
                                    },
                                    {
                                        "text": "Run away",
                                        "gotoId": 4,
                                        "consequence": {
                                            "type": "GAIN_HEALTH",
                                            "value": 2
                                        }
                                    }
                                ]
                            },
                            {
                                "id": 3,
                                "type": "NORMAL",
                                "text": "You defeated the dragon!",
                                "options": [
                                    {"text": "Continue", "gotoId": 5}
                                ]
                            },
                            {
                                "id": 4,
                                "type": "NORMAL",
                                "text": "You escaped!",
                                "options": [
                                    {"text": "Continue", "gotoId": 5}
                                ]
                            },
                            {
                                "id": 5,
                                "type": "END",
                                "text": "The adventure ends here!"
                            }
                        ]
                    }
                    """;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build test book content", e);
        }
    }

    public static String buildTestBookJson() {
        try {
            return objectMapper.writeValueAsString(buildTestBookContent());
        } catch (Exception e) {
            throw new RuntimeException("Failed to build test book JSON", e);
        }
    }

    public static JsonNode buildInvalidBookContent_NoSections() {
        try {
            String json = """
                    {
                        "id": 1,
                        "title": "Invalid Book",
                        "author": "Test Author"
                    }
                    """;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build invalid book content", e);
        }
    }

    public static JsonNode buildInvalidBookContent_NoBegin() {
        try {
            String json = """
                    {
                        "id": 1,
                        "title": "Invalid Book",
                        "author": "Test Author",
                        "sections": [
                            {
                                "id": 1,
                                "type": "NORMAL",
                                "text": "This has no BEGIN section",
                                "options": [
                                    {"text": "Continue", "gotoId": 2}
                                ]
                            },
                            {
                                "id": 2,
                                "type": "END",
                                "text": "The end"
                            }
                        ]
                    }
                    """;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build invalid book content", e);
        }
    }

    public static JsonNode buildInvalidBookContent_NoEnd() {
        try {
            String json = """
                    {
                        "id": 1,
                        "title": "Invalid Book",
                        "author": "Test Author",
                        "sections": [
                            {
                                "id": 1,
                                "type": "BEGIN",
                                "text": "Start",
                                "options": [
                                    {"text": "Continue", "gotoId": 2}
                                ]
                            },
                            {
                                "id": 2,
                                "type": "NORMAL",
                                "text": "No end section",
                                "options": [
                                    {"text": "Continue", "gotoId": 1}
                                ]
                            }
                        ]
                    }
                    """;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build invalid book content", e);
        }
    }

    public static JsonNode buildInvalidBookContent_NoOptions() {
        try {
            String json = """
                    {
                        "id": 1,
                        "title": "Invalid Book",
                        "author": "Test Author",
                        "sections": [
                            {
                                "id": 1,
                                "type": "BEGIN",
                                "text": "Start"
                            },
                            {
                                "id": 2,
                                "type": "END",
                                "text": "The end"
                            }
                        ]
                    }
                    """;
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build invalid book content", e);
        }
    }

    public static List<BookDTO> buildTestBooks() {
        return Arrays.asList(
                buildTestBook(1L, "Fantasy Quest", "EASY"),
                buildTestBook(2L, "Dark Mystery", "HARD"),
                buildTestBook(3L, "Space Adventure", "MEDIUM")
        );
    }

    public static String buildTestBookJsonString() {
        try {
            return objectMapper.writeValueAsString(buildTestBookContent());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test book content", e);
        }
    }
}
