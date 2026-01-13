package com.examportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for verification execution results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResult {

    private Boolean passed;
    private List<TestCaseResult> testCaseResults;
    private List<String> constraintViolations;
    private Integer executionTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private Boolean passed;
        private String error;
    }
}
