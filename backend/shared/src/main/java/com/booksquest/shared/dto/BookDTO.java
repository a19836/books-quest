package com.booksquest.shared.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDTO {
    private Long id;
    private String title;
    private String author;
    private String difficulty;
    private String category;
    private String description;
    private List<String> tags;
    private Integer fastReadingMinutes;
    private Integer slowReadingMinutes;
    private Integer chapters;
    private JsonNode content;
}
