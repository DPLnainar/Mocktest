package com.examportal.monitoring.dto;

public class ModeratorWarningRequest {
    private Long studentId;
    private String message;
    private Long timestamp;

    public ModeratorWarningRequest() {}
    public ModeratorWarningRequest(Long studentId, String message, Long timestamp) {
        this.studentId = studentId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
