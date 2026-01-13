package com.examportal.parser.service;

import com.examportal.parser.service.impl.JavaParserService;
import com.examportal.parser.service.impl.PythonParserService;
import org.springframework.stereotype.Component;

/**
 * Parser Factory
 * 
 * Returns appropriate parser service based on language
 * Strategy pattern for multi-language support
 */
@Component
public class ParserFactory {

    private final JavaParserService javaParserService;
    private final PythonParserService pythonParserService;

    public ParserFactory(JavaParserService javaParserService, PythonParserService pythonParserService) {
        this.javaParserService = javaParserService;
        this.pythonParserService = pythonParserService;
    }

    /**
     * Get parser for specified language
     * 
     * @param language Language identifier (JAVA, PYTHON, CPP)
     * @return Parser service for the language
     * @throws IllegalArgumentException if language not supported
     */
    public ParserService getParser(String language) {
        if (language == null) {
            throw new IllegalArgumentException("Language cannot be null");
        }

        switch (language.toUpperCase()) {
            case "JAVA":
                return javaParserService;
            case "PYTHON":
            case "PYTHON3":
                return pythonParserService;
            case "CPP":
            case "C++":
                // Future: Implement C++ parser
                throw new UnsupportedOperationException("C++ parser not yet implemented");
            default:
                throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    /**
     * Check if language is supported
     */
    public boolean isLanguageSupported(String language) {
        try {
            getParser(language);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
