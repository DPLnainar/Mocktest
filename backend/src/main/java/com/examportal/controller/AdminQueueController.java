package com.examportal.controller;

import com.examportal.dto.QueueStatsResponse;
import com.examportal.service.QueueMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin controller for queue monitoring and statistics
 */
@RestController
@RequestMapping("/api/admin/queue")
@RequiredArgsConstructor
public class AdminQueueController {

    private final QueueMonitoringService queueMonitoringService;

    /**
     * Get comprehensive queue statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<QueueStatsResponse> getQueueStats() {
        QueueStatsResponse stats = queueMonitoringService.getQueueStats();
        return ResponseEntity.ok(stats);
    }
}
