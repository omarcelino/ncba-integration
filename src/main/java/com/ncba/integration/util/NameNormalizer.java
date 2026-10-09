package com.ncba.integration.util;

import org.springframework.stereotype.Component;

@Component
public class NameNormalizer {

    public String toSentenceCase(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        String cleaned = input.trim().replaceAll("\\s+", " ").toLowerCase();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        // update for cases like south africa
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }
}
