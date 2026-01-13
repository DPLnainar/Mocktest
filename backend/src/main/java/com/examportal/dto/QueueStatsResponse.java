package com.examportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for admin queue statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatsResponse {
    private int currentDepth;
    private long processedLastHour;
    private long processedLastDay;
    private long avgProcessingTimeMs;
    private long failedCount;
    private int activeConsumers;
}
