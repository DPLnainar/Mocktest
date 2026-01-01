package com.examportal.execution.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Judge0 Language Mapper
 * 
 * Maps common language names to Judge0 language IDs
 * See: https://ce.judge0.com/#statuses-and-languages-language-get
 */
public class Judge0LanguageMapper {

    private static final Map<String, Integer> LANGUAGE_MAP = new HashMap<>();

    static {
        // Java
        LANGUAGE_MAP.put("JAVA", 62);
        LANGUAGE_MAP.put("JAVA8", 62);
        
        // Python
        LANGUAGE_MAP.put("PYTHON", 71);
        LANGUAGE_MAP.put("PYTHON3", 71);
        LANGUAGE_MAP.put("PYTHON2", 70);
        
        // C++
        LANGUAGE_MAP.put("CPP", 54);
        LANGUAGE_MAP.put("C++", 54);
        LANGUAGE_MAP.put("C++17", 54);
        
        // C
        LANGUAGE_MAP.put("C", 50);
        
        // JavaScript
        LANGUAGE_MAP.put("JAVASCRIPT", 63);
        LANGUAGE_MAP.put("NODEJS", 63);
        
        // Other languages
        LANGUAGE_MAP.put("RUBY", 72);
        LANGUAGE_MAP.put("GO", 60);
        LANGUAGE_MAP.put("RUST", 73);
        LANGUAGE_MAP.put("KOTLIN", 78);
        LANGUAGE_MAP.put("SWIFT", 83);
    }

    /**
     * Get Judge0 language ID from language name
     * 
     * @param language Language name (case-insensitive)
     * @return Judge0 language ID
     * @throws IllegalArgumentException if language not supported
     */
    public static Integer getLanguageId(String language) {
        if (language == null) {
            throw new IllegalArgumentException("Language cannot be null");
        }

        Integer languageId = LANGUAGE_MAP.get(language.toUpperCase());
        if (languageId == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }

        return languageId;
    }

    /**
     * Check if language is supported
     */
    public static boolean isSupported(String language) {
        return language != null && LANGUAGE_MAP.containsKey(language.toUpperCase());
    }

    /**
     * Get all supported languages
     */
    public static Map<String, Integer> getSupportedLanguages() {
        return new HashMap<>(LANGUAGE_MAP);
    }
}
