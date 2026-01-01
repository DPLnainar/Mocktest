package com.examportal.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public enum RuleType {
        FORBIDDEN,  // Construct must NOT be present
        REQUIRED    // Construct MUST be present
    }
}
