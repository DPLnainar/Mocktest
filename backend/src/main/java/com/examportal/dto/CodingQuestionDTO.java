package com.examportal.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class CodingQuestionDTO extends QuestionDTO {

    private List<Integer> allowedLanguageIds;
    private List<Map<String, String>> testCases;
    private Map<String, Boolean> constraints;
    private String starterCode;
}
