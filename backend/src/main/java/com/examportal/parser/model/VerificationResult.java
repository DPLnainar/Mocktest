package com.examportal.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {

    private boolean passed;
    private boolean hasSyntaxErrors;
    private String syntaxErrorMessage;
    private long parsingTimeMs;
    
    @Builder.Default
    private List<Violation> violations = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Violation {
        private String type;
        private String message;
        private int lineNumber; // Changed from 'line' to match builder calls
        private String code;
        private Object rule; // VerificationRule object
    }
}
