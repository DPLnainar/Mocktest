package com.examportal.dto;

import lombok.Data;

@Data
public class AnswerSubmissionDTO {
    private Long questionId;
    private String answer; // MCQ option or code
}
