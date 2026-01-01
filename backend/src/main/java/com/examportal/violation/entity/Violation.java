package com.examportal.violation.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@Builder // Added this
@NoArgsConstructor // Added this
@AllArgsConstructor // Added this
public class Violation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long studentId;
    private Long examId;
    private String violationType;
    private String screenshotUrl;
    private String message;
    private LocalDateTime timestamp;
}
