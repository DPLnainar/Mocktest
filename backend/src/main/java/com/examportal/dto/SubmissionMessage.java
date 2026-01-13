package com.examportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionMessage implements Serializable {
    private String executionId;
    private Long attemptId;
    private Long questionId;
    private Long studentId;
    private String code;
    private Integer languageId;
    private String stdin;
    private Map<String, Boolean> constraints;
}
