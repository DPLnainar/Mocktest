package com.examportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for queue position information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueuePositionResponse {
    private int position;
    private int estimatedWaitSeconds;
    private int queueDepth;
}
