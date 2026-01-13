package com.examportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for moderator solution verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {

    private String code;
    private Integer languageId;
    private List<TestCaseData> testCases;
    private ConstraintsConfig constraints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseData {
        private String input;
        private String expectedOutput;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConstraintsConfig {
        private Boolean forbidLoops;
        private Boolean requireRecursion;
    }
}
