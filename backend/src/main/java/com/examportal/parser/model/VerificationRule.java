package com.examportal.parser.model;

/**
 * Verification Rule
 * 
 * Defines forbidden or required constructs in submitted code
 * 
 * Examples:
 * - FORBIDDEN: "Arrays.sort" when question requires Bubble Sort
 * - REQUIRED: "recursion" when question mandates recursive solution
 * - FORBIDDEN: "for loop" when question requires streams
 */
public class VerificationRule {

    /**
     * Rule type: FORBIDDEN or REQUIRED
     */
    private RuleType type;

    /**
     * Construct to check (e.g., "Arrays.sort", "recursion", "loop")
     */
    private String construct;

    /**
     * Human-readable description
     */
    private String description;

    /**
     * Error message to show if rule is violated
     */
    private String errorMessage;

    public VerificationRule() {}

    public VerificationRule(RuleType type, String construct, String description, String errorMessage) {
        this.type = type;
        this.construct = construct;
        this.description = description;
        this.errorMessage = errorMessage;
    }

    public RuleType getType() { return type; }
    public void setType(RuleType type) { this.type = type; }
    public String getConstruct() { return construct; }
    public void setConstruct(String construct) { this.construct = construct; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public enum RuleType {
        FORBIDDEN,  // Construct must NOT be present
        REQUIRED    // Construct MUST be present
    }
}
