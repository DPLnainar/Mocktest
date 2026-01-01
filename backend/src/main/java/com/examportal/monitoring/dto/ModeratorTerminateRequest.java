package com.examportal.monitoring.dto;

public class ModeratorTerminateRequest {
    private Long studentId;
    private String reason;
    private Long timestamp;

    public ModeratorTerminateRequest() {}
    public ModeratorTerminateRequest(Long studentId, String reason, Long timestamp) {
        this.studentId = studentId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
