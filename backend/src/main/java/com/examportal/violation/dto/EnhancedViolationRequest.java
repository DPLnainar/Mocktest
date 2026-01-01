package com.examportal.violation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phase 8: Enhanced ViolationReportRequest with consecutive frame tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedViolationRequest {
    
    private Long sessionId;
    private Long studentId;
    private Long examId;
    private String violationType;
    private String severity;
    private String message;
    private Object evidence;
    
    // Phase 8: Consecutive frame metadata
    private Integer consecutiveFrames; // Number of consecutive frames detected
    private Double confidence; // Confidence score (0.0 - 1.0)
    private Boolean confirmed; // True if passed 3-frame threshold
}
