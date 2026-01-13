package com.examportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for verification request submission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {

    private String verificationId;
    private VerificationStatus status;
    private String message;
    private VerificationResult result;

    public enum VerificationStatus {
        QUEUED,
        PROCESSING,
        SUCCESS,
        FAILED,
        ERROR
    }
}
