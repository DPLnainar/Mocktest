package com.examportal.execution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Added this
@NoArgsConstructor // Added this
@AllArgsConstructor // Added this
public class Judge0SubmissionRequest {
    private String source_code;
    private int language_id;
    private String stdin;
    private String expected_output;
    private Double cpu_time_limit;
    private Double memory_limit;
}
