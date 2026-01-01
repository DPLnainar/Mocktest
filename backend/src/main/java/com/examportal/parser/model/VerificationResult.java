package com.examportal.parser.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Verification Result
 * 
 * Contains the outcome of code logic verification
 */
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

    public VerificationResult() {}

    public VerificationResult(boolean passed, long parsingTimeMs, List<Violation> violations, boolean hasSyntaxErrors, String syntaxErrorMessage) {
        this.passed = passed;
        this.parsingTimeMs = parsingTimeMs;
        this.violations = violations;
        this.hasSyntaxErrors = hasSyntaxErrors;
        this.syntaxErrorMessage = syntaxErrorMessage;
    }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public long getParsingTimeMs() { return parsingTimeMs; }
    public void setParsingTimeMs(long parsingTimeMs) { this.parsingTimeMs = parsingTimeMs; }
    public List<Violation> getViolations() { return violations; }
    public void setViolations(List<Violation> violations) { this.violations = violations; }
    public boolean isHasSyntaxErrors() { return hasSyntaxErrors; }
    public void setHasSyntaxErrors(boolean hasSyntaxErrors) { this.hasSyntaxErrors = hasSyntaxErrors; }
    public String getSyntaxErrorMessage() { return syntaxErrorMessage; }
    public void setSyntaxErrorMessage(String syntaxErrorMessage) { this.syntaxErrorMessage = syntaxErrorMessage; }

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

        public Violation() {}

        public Violation(VerificationRule rule, int lineNumber, String codeSnippet, String message) {
            this.rule = rule;
            this.lineNumber = lineNumber;
            this.codeSnippet = codeSnippet;
            this.message = message;
        }

        public VerificationRule getRule() { return rule; }
        public void setRule(VerificationRule rule) { this.rule = rule; }
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        public String getCodeSnippet() { return codeSnippet; }
        public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
