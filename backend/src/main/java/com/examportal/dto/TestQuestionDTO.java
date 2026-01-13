package com.examportal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "handler" })
public class TestQuestionDTO {
    private Long id;
    private Long questionId;
    private QuestionDTO question; // Full question DTO (polymorphic)
    private Integer marks;
    private String sectionName;
    private Integer orderIndex;
}
