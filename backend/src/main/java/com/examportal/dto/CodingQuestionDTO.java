package com.examportal.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodingQuestionDTO extends QuestionDTO {

    private List<Integer> allowedLanguageIds;
    private List<Map<String, String>> testCases;
    private Map<String, Boolean> constraints;
    private String starterCode;
}
