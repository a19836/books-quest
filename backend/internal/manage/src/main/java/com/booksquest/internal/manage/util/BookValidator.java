package com.booksquest.internal.manage.util;

import com.booksquest.internal.manage.exception.BookValidationException;
import com.fasterxml.jackson.databind.JsonNode;

public class BookValidator {

    public static void validateBook(JsonNode bookJson) {
        JsonNode sections = bookJson.get("sections");

        if (sections == null || !sections.isArray()) {
            throw new BookValidationException("Book must have sections array");
        }

        int beginCount = 0;
        int endCount = 0;

        for (JsonNode section : sections) {
            String type = section.get("type").asText();

            if ("BEGIN".equals(type)) {
                beginCount++;
            } else if ("END".equals(type)) {
                endCount++;
            }
        }

        if (beginCount != 1) {
            throw new BookValidationException("Book must have exactly one BEGIN section");
        }

        if (endCount == 0) {
            throw new BookValidationException("Book must have at least one END section");
        }

        validateGotoReferences(bookJson);
        validateSectionOptions(bookJson);
    }

    private static void validateGotoReferences(JsonNode bookJson) {
        JsonNode sections = bookJson.get("sections");

        java.util.Set<Integer> validIds = new java.util.HashSet<>();
        for (JsonNode section : sections) {
            JsonNode idNode = section.get("id");
            if (idNode.isNumber()) {
                validIds.add(idNode.asInt());
            } else {
                validIds.add(Integer.parseInt(idNode.asText()));
            }
        }

        for (JsonNode section : sections) {
            JsonNode options = section.get("options");
            if (options != null && options.isArray()) {
                for (JsonNode option : options) {
                    JsonNode gotoNode = option.get("gotoId");
                    if (gotoNode != null) {
                        int gotoId = gotoNode.isNumber() ? gotoNode.asInt() : Integer.parseInt(gotoNode.asText());
                        if (!validIds.contains(gotoId)) {
                            throw new BookValidationException("Invalid gotoId reference: " + gotoId);
                        }
                    }
                }
            }
        }
    }

    private static void validateSectionOptions(JsonNode bookJson) {
        JsonNode sections = bookJson.get("sections");

        for (JsonNode section : sections) {
            String type = section.get("type").asText();

            if ("END".equals(type)) {
                continue;
            }

            JsonNode options = section.get("options");
            if (options == null || !options.isArray() || options.size() == 0) {
                JsonNode idNode = section.get("id");
                String sectionId = idNode.isNumber() ? String.valueOf(idNode.asInt()) : idNode.asText();
                throw new BookValidationException("Non-ending section " + sectionId + " has no options");
            }
        }
    }
}
