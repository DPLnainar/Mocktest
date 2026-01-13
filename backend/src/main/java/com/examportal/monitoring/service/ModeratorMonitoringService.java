package com.examportal.monitoring.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ModeratorMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(ModeratorMonitoringService.class);

    private final SessionManagerService sessionManager;

    public ModeratorMonitoringService(SessionManagerService sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void handleModeratorConnect(String moderatorId, Long examId) {
        log.info("Moderator {} connected to exam {}", moderatorId, examId);
    }

    public void terminateStudent(Long sessionId, String reason, String moderatorId) {
        log.warn("Moderator {} terminating session {} - Reason: {}", moderatorId, sessionId, reason);
        sessionManager.terminateSession(sessionId);
    }

    public void sendWarning(Long sessionId, String message, String moderatorId) {
        log.info("Moderator {} sending warning to session {}: {}", moderatorId, sessionId, message);
        // Warning logic handled by broadcast service, SessionManager update not
        // strictly required here
    }
}
