package com.examportal.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TestDTO {
    
    private Long id;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    // Department will be set from authenticated user, not from request
    private String department;
    
    @NotNull(message = "Start date time is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDateTime;
    
    @NotNull(message = "End date time is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDateTime;
    
    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;
    
    private List<Long> questionIds;
    private List<QuestionDTO> questions; // Full question objects for frontend
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
