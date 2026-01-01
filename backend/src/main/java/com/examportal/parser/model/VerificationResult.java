package com.examportal.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {

    private boolean passed; // Was 'verified', renamed to 'passed' to match Service
    private boolean hasSyntaxErrors;
    private String syntaxErrorMessage;
    private long parsingTimeMs;
    
    // Initialize with empty list to avoid NullPointer if not set
    @Builder.Default
    private List<Violation> violations = new ArrayList<>(); 

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Violation {
        private String type;
        private String message;
        private int line;
        private String code;
    }
}
