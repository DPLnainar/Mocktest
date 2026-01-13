package com.examportal.violation.listener;

import com.examportal.violation.event.ViolationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Violation Event Listener
 * 
 * Async event handler for violation processing
 * Handles:
 * - Logging
 * - Analytics
 * - External notifications (future: email, SMS)
 * - ML model training data (future)
 */
@Component
public class ViolationEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViolationEventListener.class);

    /**
     * Handle violation event asynchronously
     * Runs in separate thread pool to avoid blocking main flow
     */
    @EventListener
    @Async("taskExecutor")
    public void handleViolationEvent(ViolationEvent event) {
        log.info("Processing violation event: {} for student {} (Session: {})",
                event.getType(), event.getStudentId(), event.getSessionId());

        try {
            // Log for analytics
            logViolationForAnalytics(event);

            // Future: Send notification to exam coordinator (if critical)
            if (event.getSeverity() == com.examportal.violation.entity.Violation.Severity.CRITICAL) {
                notifyExamCoordinator(event);
            }

            // Future: Store evidence to external storage (S3, etc.)
            if (event.getEvidence() != null && !event.getEvidence().isEmpty()) {
                archiveEvidence(event);
            }

        } catch (Exception e) {
            log.error("Error processing violation event", e);
        }
    }

    /**
     * Log violation for analytics/ML
     */
    private void logViolationForAnalytics(ViolationEvent event) {
        // Could send to analytics service, data warehouse, etc.
        log.debug("Violation analytics: Type={}, Severity={}, Student={}, Exam={}",
                event.getType(), event.getSeverity(), event.getStudentId(), event.getExamId());
    }

    /**
     * Notify exam coordinator of critical violations
     */
    private void notifyExamCoordinator(ViolationEvent event) {
        // Future: Implement email/SMS notification
        log.warn("CRITICAL VIOLATION: {} for student {} in session {}",
                event.getType(), event.getStudentId(), event.getSessionId());
    }

    /**
     * Archive evidence to external storage
     */
    private void archiveEvidence(ViolationEvent event) {
        // Future: Upload screenshot to S3/Cloud Storage
        log.debug("Archiving evidence for violation in session {}", event.getSessionId());
    }
}
