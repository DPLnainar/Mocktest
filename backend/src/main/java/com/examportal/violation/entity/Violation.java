package com.examportal.violation.entity;

import com.examportal.violation.converter.MapToJsonConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

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
    private Long sessionId;
    private Long examId;
    
    @Enumerated(EnumType.STRING)
    private ViolationType type;
    
    @Enumerated(EnumType.STRING)
    private ViolationType violationType;
    
    private String screenshotUrl;
    private String message;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = MapToJsonConverter.class)
    private Map<String, Object> evidence;
    
    @Enumerated(EnumType.STRING)
    private Severity severity;
    
    private LocalDateTime timestamp;
    private LocalDateTime detectedAt;
    
    private Boolean confirmed = false;
    private Integer strikeCount = 1;

    // These Enums were missing!
    public enum ViolationType {
        TAB_SWITCH,
        MULTIPLE_FACES,
        NO_FACE,
        NO_FACE_DETECTED,
        UNKNOWN_FACE,
        MOBILE_DETECTED,
        PHONE_DETECTED,
        PROHIBITED_OBJECT,
        COPY_PASTE_DETECTED
    }

    public enum Severity {
        LOW,
        MINOR,
        MEDIUM,
        MAJOR,
        HIGH,
        CRITICAL
    }
}
