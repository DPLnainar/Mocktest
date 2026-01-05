package com.examportal.dto;

import com.examportal.entity.ViolationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResultDTO {
    private Long studentId;
    private String studentName;
    private String registrationNumber;
    private Double score;
    private Double totalMarks;
    private Double percentage;
    private LocalDateTime submittedAt;
    private Integer violationCount;
    private Boolean autoSubmitted;
    private Map<ViolationType, Long> violationSummary;
    private AttendanceStatus attendanceStatus;
    
    public enum AttendanceStatus {
        ATTENDED,
        NOT_ATTENDED
    }
}
