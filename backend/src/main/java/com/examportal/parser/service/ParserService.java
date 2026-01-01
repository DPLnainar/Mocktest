package com.examportal.parser.service;

import com.examportal.parser.model.VerificationResult;
import com.examportal.parser.model.VerificationRule;

import java.util.List;

/**
 * Parser Service Interface
 * 
 * Provides code parsing and logic verification for different programming languages
 * Implementations: JavaParserService, PythonParserService
 */
public interface ParserService {

    /**
     * Parse and verify code against specified rules
     * 
     * @param code Source code to verify
     * @param rules List of verification rules to check
     * @return Verification result with violations found
     */
    VerificationResult verifyCode(String code, List<VerificationRule> rules);

    /**
     * Check if code contains syntax errors
     * 
     * @param code Source code to check
     * @return true if code has syntax errors
     */
    boolean hasSyntaxErrors(String code);

    /**
     * Get supported language
     * 
     * @return Language identifier (JAVA, PYTHON, CPP)
     */
    String getSupportedLanguage();
}
