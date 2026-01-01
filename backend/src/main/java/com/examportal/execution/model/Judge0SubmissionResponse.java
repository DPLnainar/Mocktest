package com.examportal.execution.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Judge0SubmissionResponse {
    private String token;
    private Status status;
    private String stdout;
    private String stderr;
    private String compile_output;
    private String message;
    private Integer exit_code;
    private String exit_signal;
    private Double time;
    private Double memory;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        private Integer id;
        private String description;
    }
    
    // NO manual getters/setters below this line!
}
