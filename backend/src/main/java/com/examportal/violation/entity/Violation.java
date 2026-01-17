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

    @Builder.Default
    private Boolean confirmed = false;
    @Builder.Default
    private Integer strikeCount = 1;

    // Violation Types
    public enum ViolationType {
        // Tab/Window violations
        TAB_SWITCH,
        WINDOW_BLUR,
        FULLSCREEN_EXIT,

        // Face detection violations
        MULTIPLE_FACES,
        MULTIPLE_FACES_DETECTED,
        NO_FACE,
        NO_FACE_DETECTED,
        UNKNOWN_FACE,

        // Device/Object detection
        MOBILE_DETECTED,
        PHONE_DETECTED,
        CAMERA_DETECTED,
        PROHIBITED_OBJECT,
        UNAUTHORIZED_OBJECT_DETECTED,

        // Copy/Paste violations
        COPY_PASTE_DETECTED,
        LARGE_PASTE,
        SUSPICIOUS_TYPING_SPEED,
        RAPID_ANSWER_CHANGES,

        // Screenshot/Screen capture
        SCREENSHOT_ATTEMPT,
        SCREEN_CAPTURE_DETECTED,

        // Extension/Browser violations
        SUSPICIOUS_EXTENSION_DETECTED,

        // Network violations
        IP_TRACKED,
        IP_ADDRESS_CHANGED
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
