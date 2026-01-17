package com.examportal.dto;

import com.examportal.entity.QuestionType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MCQQuestionDTO.class, name = "MCQ"),
        @JsonSubTypes.Type(value = CodingQuestionDTO.class, name = "CODING")
})
public abstract class QuestionDTO {

    private Long id;

    @NotNull(message = "Question type is required")
    private QuestionType type;

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotNull(message = "Marks is required")
    @Positive(message = "Marks must be positive")
    private Integer marks;

    private String department;

    private String explanation;
}
