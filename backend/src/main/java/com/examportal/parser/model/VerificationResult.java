package com.examportal.parser.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {

    private boolean verified;
    private List<Violation> violations;

    @Data
    @Builder // Added this
    @NoArgsConstructor // Added this
    @AllArgsConstructor // Added this
    public static class Violation {
        private String type;
        private String message;
        private int line;
        private String code;
    }
}
