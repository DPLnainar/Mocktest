package com.examportal.monitoring.controller;

import com.examportal.monitoring.dto.ModeratorConnectRequest;
import com.examportal.monitoring.dto.ModeratorTerminateRequest;
import com.examportal.monitoring.dto.ModeratorWarningRequest;
import com.examportal.monitoring.service.ModeratorMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * ModeratorWebSocketController
 * 
 * WebSocket endpoints for moderator dashboard
 * Handles moderator connections, terminations, and warnings
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ModeratorWebSocketController {

    private final ModeratorMonitoringService moderatorService;

    /**
     * Moderator connects to exam
     * Sends initial batch of student status
     */
    @MessageMapping("/monitoring/moderator/connect")
    public void handleModeratorConnect(
            @Payload ModeratorConnectRequest request,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal
    ) {
        String moderatorId = principal.getName();
        log.info("Moderator {} connected to exam {}", moderatorId, request.getExamId());

        // Store moderator session
        headerAccessor.getSessionAttributes().put("moderatorId", moderatorId);
        headerAccessor.getSessionAttributes().put("examId", request.getExamId());

        // Send initial student data
        moderatorService.handleModeratorConnect(moderatorId, request.getExamId());
    }

    /**
     * Terminate student
     */
    @MessageMapping("/monitoring/moderator/terminate")
    public void handleTermination(
            @Payload ModeratorTerminateRequest request,
            Principal principal
    ) {
        String moderatorId = principal.getName();
        log.info("Moderator {} terminating student {}: {}", 
                moderatorId, request.getStudentId(), request.getReason());

        moderatorService.terminateStudent(
                request.getStudentId(),
                moderatorId,
                request.getReason()
        );
    }

    /**
     * Send warning to student
     */
    @MessageMapping("/monitoring/moderator/warning")
    public void handleWarning(
            @Payload ModeratorWarningRequest request,
            Principal principal
    ) {
        String moderatorId = principal.getName();
        log.info("Moderator {} sending warning to student {}", 
                moderatorId, request.getStudentId());

        moderatorService.sendWarning(
                request.getStudentId(),
                moderatorId,
                request.getMessage()
        );
    }
}
