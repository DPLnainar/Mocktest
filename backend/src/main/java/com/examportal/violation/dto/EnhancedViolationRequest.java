package com.examportal.violation.dto;

/**
 * Phase 8: Enhanced ViolationReportRequest with consecutive frame tracking
 */
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

    public EnhancedViolationRequest() {}

    public EnhancedViolationRequest(Long sessionId, Long studentId, Long examId, String violationType, String severity, String message, Object evidence, Integer consecutiveFrames, Double confidence, Boolean confirmed) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.examId = examId;
        this.violationType = violationType;
        this.severity = severity;
        this.message = message;
        this.evidence = evidence;
        this.consecutiveFrames = consecutiveFrames;
        this.confidence = confidence;
        this.confirmed = confirmed;
    }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getEvidence() { return evidence; }
    public void setEvidence(Object evidence) { this.evidence = evidence; }
    public Integer getConsecutiveFrames() { return consecutiveFrames; }
    public void setConsecutiveFrames(Integer consecutiveFrames) { this.consecutiveFrames = consecutiveFrames; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Boolean getConfirmed() { return confirmed; }
    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
}
