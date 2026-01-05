package com.examportal.dto;

import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeVerificationResult {
    private boolean valid;
    
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    private String message;
    
    public static CodeVerificationResult success() {
        return CodeVerificationResult.builder()
                .valid(true)
                .message("Code validation passed")
                .build();
    }
    
    public static CodeVerificationResult failure(List<String> errors) {
        return CodeVerificationResult.builder()
                .valid(false)
                .errors(errors)
                .message("Code validation failed")
                .build();
    }
}
