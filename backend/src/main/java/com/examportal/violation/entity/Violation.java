package com.examportal.violation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Violation Entity
 * 
 * Stores violation records with evidence in PostgreSQL
 * Evidence stored as JSONB for flexibility (screenshots, metadata)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "violations", indexes = {
    @Index(name = "idx_violation_session", columnList = "session_id, detected_at"),
    @Index(name = "idx_violation_student", columnList = "student_id, detected_at"),
    @Index(name = "idx_violation_exam", columnList = "exam_id, detected_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Violation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Exam session ID
     */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /**
     * Student ID
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * Exam ID
     */
    @Column(name = "exam_id", nullable = false)
    private Long examId;

    /**
     * Violation type
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ViolationType type;

    /**
     * Severity level (determines strike count)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    /**
     * Description of violation
     */
    @Column(length = 500)
    private String description;

    /**
     * Evidence metadata (JSONB)
     * Contains: screenshot base64, detection confidence, object detected, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> evidence;

    /**
     * Whether this violation was confirmed (not a false positive)
     */
    @Column(nullable = false)
    private Boolean confirmed = true;

    /**
     * Detection timestamp
     */
    @CreatedDate
    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    /**
     * Strike count for this violation (1-2)
     */
    @Column(nullable = false)
    private Integer strikeCount = 1;

    /**
     * Violation types
     */
    public enum ViolationType {
        // Camera violations
        MULTIPLE_FACES,
        NO_FACE_DETECTED,
        PHONE_DETECTED,
        
        // Tab/window violations
        TAB_SWITCH,
        WINDOW_BLUR,
        FULLSCREEN_EXIT,
        
        // Code violations
        COPY_PASTE_DETECTED,
        FORBIDDEN_CONSTRUCT,
        
        // Other
        MANUAL_FLAG,
        SUSPICIOUS_ACTIVITY
    }

    /**
     * Severity determines strike count
     */
    public enum Severity {
        MINOR(1),      // 1 strike (brief face absence, look away)
        MAJOR(2),      // 2 strikes (phone, multiple faces, tab switch)
        CRITICAL(5);   // Immediate termination (AI IDE detected, code copy)

        private final int strikeCount;

        Severity(int strikeCount) {
            this.strikeCount = strikeCount;
        }

        public int getStrikeCount() {
            return strikeCount;
        }
    }

    /**
     * Calculate strike count from severity
     */
    @PrePersist
    @PreUpdate
    public void calculateStrikeCount() {
        if (severity != null) {
            this.strikeCount = severity.getStrikeCount();
        }
    }
}
