package com.examportal.dto;

import com.examportal.entity.QuestionType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class QuestionDTO {
    
    private Long id;
    
    @NotNull(message = "Question type is required")
    private QuestionType type;
    
    @NotBlank(message = "Question text is required")
    private String questionText;
    
    @NotNull(message = "Marks is required")
    @Positive(message = "Marks must be positive")
    private Integer marks;
    
    private String department;
    
    // MCQ fields
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption; // A, B, C, or D
    
    // Coding fields
    private Integer languageId;
    private List<Map<String, String>> testCases;
    private String starterCode;
    
    private String explanation;
}
