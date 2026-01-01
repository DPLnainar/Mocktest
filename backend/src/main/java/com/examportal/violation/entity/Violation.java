package com.examportal.violation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Violation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long studentId;
    private Long examId;
    
    @Enumerated(EnumType.STRING)
    private ViolationType violationType;
    
    private String screenshotUrl;
    private String message;
    
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    private LocalDateTime timestamp;

    // These Enums were missing!
    public enum ViolationType {
        TAB_SWITCH,
        MULTIPLE_FACES,
        NO_FACE,
        UNKNOWN_FACE,
        MOBILE_DETECTED,
        PROHIBITED_OBJECT
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
