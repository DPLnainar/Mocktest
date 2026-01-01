package com.examportal.execution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder // Added this
@NoArgsConstructor // Added this
@AllArgsConstructor // Added this
public class ExecutionResult {
    private String output;
    private String error;
    private String status; // "ACCEPTED", "WRONG_ANSWER", "COMPILE_ERROR"
    private double time;
    private double memory;
    private String message;
}
