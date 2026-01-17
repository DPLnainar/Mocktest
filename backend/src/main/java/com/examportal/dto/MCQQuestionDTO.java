package com.examportal.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MCQQuestionDTO extends QuestionDTO {

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption; // A, B, C, or D
}
