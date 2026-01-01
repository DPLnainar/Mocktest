package com.examportal.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Verification Result
 * 
 * Contains the outcome of code logic verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {

    /**
     * Whether code passed all verification rules
     */
    private boolean passed;

    /**
     * Time taken to parse (milliseconds)
     */
    private long parsingTimeMs;

    /**
     * List of violations found
     */
    private List<Violation> violations = new ArrayList<>();

    /**
     * Whether code has syntax errors
     */
    private boolean hasSyntaxErrors;

    /**
     * Syntax error message (if any)
     */
    private String syntaxErrorMessage;

    /**
     * Add a violation to the result
     */
    public void addViolation(Violation violation) {
        if (violations == null) {
            violations = new ArrayList<>();
        }
        violations.add(violation);
        this.passed = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Violation {
        /**
         * Rule that was violated
         */
        private VerificationRule rule;

        /**
         * Line number where violation occurred
         */
        private int lineNumber;

        /**
         * Code snippet at violation location
         */
        private String codeSnippet;

        /**
         * Detailed violation message
         */
        private String message;
    }
}
