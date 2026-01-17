package com.examportal.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestDTO {

    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    // Department will be set from authenticated user, not from request
    private String department;

    @NotNull(message = "Start date time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDateTime;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;

    @NotNull(message = "Test type is required")
    private String type; // MCQ_ONLY, CODING_ONLY, HYBRID

    private String status; // DRAFT, PUBLISHED, ARCHIVED

    private String testType; // "Placement Drive", "Practice", "Contest"

    private String instructions; // Rich text instructions for students

    @Builder.Default
    private List<TestQuestionDTO> testQuestions = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
