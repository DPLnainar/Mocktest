package com.examportal.monitoring.controller;

import com.examportal.monitoring.model.ExamSession;
import com.examportal.monitoring.model.StudentStatus;
import com.examportal.monitoring.service.MonitoringBroadcastService;
import com.examportal.monitoring.service.SessionManagerService;
import com.examportal.security.CustomUserDetails;
import com.examportal.security.DepartmentSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Monitoring REST Controller
 * 
 * REST endpoints for exam monitoring (complement to WebSocket)
 * Used for initial data load and HTTP-based operations
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringRestController {

    private static final Logger log = LoggerFactory.getLogger(MonitoringRestController.class);

    private final SessionManagerService sessionManager;
    private final MonitoringBroadcastService broadcastService;
    private final DepartmentSecurityService securityService;

    public MonitoringRestController(SessionManagerService sessionManager, 
                                    MonitoringBroadcastService broadcastService, 
                                    DepartmentSecurityService securityService) {
        this.sessionManager = sessionManager;
        this.broadcastService = broadcastService;
        this.securityService = securityService;
    }

    /**
     * Get all active sessions for an exam
     * 
     * GET /api/monitoring/exam/{examId}/sessions
     */
    @GetMapping("/exam/{examId}/sessions")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<StudentStatus>> getExamSessions(
            @PathVariable Long examId,
            @AuthenticationPrincipal CustomUserDetails moderator) {
        
        log.info("Moderator {} requesting sessions for exam {}", moderator.getId(), examId);

        // Get active sessions
        var sessions = sessionManager.getActiveSessionsForExam(examId);
        
        // Filter by department unless admin
        if (!securityService.isCurrentUserAdmin()) {
            String moderatorDept = moderator.getDepartment();
            sessions = sessions.stream()
                    .filter(s -> moderatorDept.equals(s.getDepartment()))
                    .collect(java.util.stream.Collectors.toSet());
        }

        // Convert to StudentStatus
        List<StudentStatus> statusList = sessions.stream()
                .map(this::convertToStudentStatus)
                .toList();

        return ResponseEntity.ok(statusList);
    }

    /**
     * Get sessions for moderator's department
     * 
     * GET /api/monitoring/department/sessions
     */
    @GetMapping("/department/sessions")
    @PreAuthorize("hasRole('MODERATOR')")
    public ResponseEntity<List<StudentStatus>> getDepartmentSessions(
            @AuthenticationPrincipal CustomUserDetails moderator) {
        
        String department = moderator.getDepartment();
        log.info("Getting active sessions for department {}", department);

        var sessions = sessionManager.getActiveSessionsForDepartment(department);
        
        List<StudentStatus> statusList = sessions.stream()
                .map(this::convertToStudentStatus)
                .toList();

        return ResponseEntity.ok(statusList);
    }

    /**
     * Manually terminate a student's exam
     * 
     * POST /api/monitoring/session/{sessionId}/terminate
     */
    @PostMapping("/session/{sessionId}/terminate")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<String> terminateSession(
            @PathVariable Long sessionId,
            @RequestBody TerminationRequest request,
            @AuthenticationPrincipal CustomUserDetails moderator) {
        
        ExamSession session = sessionManager.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        // Check department access (unless admin)
        if (!securityService.isCurrentUserAdmin()) {
            securityService.verifyDepartmentAccess(session.getDepartment());
        }

        log.warn("Moderator {} terminating session {} for student {}: {}", 
                moderator.getId(), sessionId, session.getStudentId(), request.reason());

        // Terminate session
        sessionManager.terminateSession(sessionId);

        // Notify student
        broadcastService.sendToStudent(
                session.getStudentId(), 
                "termination",
                new TerminationNotice(request.reason(), LocalDateTime.now())
        );

        // Broadcast to moderators
        broadcastService.broadcastTermination(session.getExamId(), session.getStudentId(), request.reason());

        return ResponseEntity.ok("Session terminated successfully");
    }

    /**
     * Get monitoring statistics
     * 
     * GET /api/monitoring/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<MonitoringStats> getMonitoringStats() {
        long activeCount = sessionManager.getActiveSessionCount();
        
        MonitoringStats stats = new MonitoringStats(
                activeCount,
                0L, // TODO: Get from violation service
                0L  // TODO: Get terminated count
        );

        return ResponseEntity.ok(stats);
    }

    private StudentStatus convertToStudentStatus(ExamSession session) {
        StudentStatus status = new StudentStatus();
        status.setStudentId(session.getStudentId());
        status.setStudentName(session.getStudentName());
        status.setEmail(null);
        status.setSessionId(session.getId());
        status.setConnectionStatus(session.isHeartbeatStale() ? 
                StudentStatus.ConnectionStatus.OFFLINE : 
                StudentStatus.ConnectionStatus.ONLINE);
        status.setActivityStatus(StudentStatus.ActivityStatus.IDLE);
        status.setViolationCount(session.getViolationCount());
        // Calculate status color based on violation count
        int violations = session.getViolationCount() != null ? session.getViolationCount() : 0;
        if (violations > 5) {
            status.setStatusColor(StudentStatus.StatusColor.RED);
        } else if (violations > 2) {
            status.setStatusColor(StudentStatus.StatusColor.YELLOW);
        } else {
            status.setStatusColor(StudentStatus.StatusColor.GREEN);
        }
        status.setLastActivity(session.getLastHeartbeat());
        status.setCurrentQuestion(null);
        status.setCompletedQuestions(null);
        status.setCameraStatus(StudentStatus.CameraStatus.ACTIVE);
        status.setTabSwitchCount(0);
        return status;
    }

    // DTOs
    public record TerminationRequest(String reason) {}
    public record TerminationNotice(String reason, LocalDateTime terminatedAt) {}
    public record MonitoringStats(long activeSessions, long totalViolations, long terminatedSessions) {}
}
