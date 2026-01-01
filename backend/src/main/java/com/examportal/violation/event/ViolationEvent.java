package com.examportal.violation.event;

import com.examportal.violation.entity.Violation;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * Violation Event
 * 
 * Published when a violation is detected
 * Triggers async processing, broadcasting, and auto-termination check
 */
public class ViolationEvent extends ApplicationEvent {

    private final Long sessionId;
    private final Long studentId;
    private final Long examId;
    private final Violation.ViolationType type;
    private final Violation.Severity severity;
    private final String description;
    private final Map<String, Object> evidence;

    public ViolationEvent(Object source, Long sessionId, Long studentId, Long examId,
                         Violation.ViolationType type, Violation.Severity severity,
                         String description, Map<String, Object> evidence) {
        super(source);
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.examId = examId;
        this.type = type;
        this.severity = severity;
        this.description = description;
        this.evidence = evidence;
    }

    public Long getSessionId() { return sessionId; }
    public Long getStudentId() { return studentId; }
    public Long getExamId() { return examId; }
    public Violation.ViolationType getType() { return type; }
    public Violation.Severity getSeverity() { return severity; }
    public String getDescription() { return description; }
    public Map<String, Object> getEvidence() { return evidence; }
}
