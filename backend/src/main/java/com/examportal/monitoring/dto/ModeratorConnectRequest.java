package com.examportal.monitoring.dto;

public class ModeratorConnectRequest {
    private Long examId;

    public ModeratorConnectRequest() {}
    public ModeratorConnectRequest(Long examId) {
        this.examId = examId;
    }

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
}
