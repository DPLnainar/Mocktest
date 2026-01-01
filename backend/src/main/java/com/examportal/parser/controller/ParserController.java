package com.examportal.parser.controller;

import com.examportal.parser.model.VerificationResult;
import com.examportal.parser.model.VerificationRule;
import com.examportal.parser.service.ParserFactory;
import com.examportal.parser.service.ParserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Parser Controller
 * 
 * Provides API endpoints for code verification
 * Used by exam submission service to verify logic integrity
 */
@RestController
@RequestMapping("/api/parser")
@RequiredArgsConstructor
@Slf4j
public class ParserController {

    private final ParserFactory parserFactory;

    /**
     * Verify code against rules
     * 
     * POST /api/parser/verify
     */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('STUDENT', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<VerificationResult> verifyCode(@Valid @RequestBody VerifyRequest request) {
        log.info("Verifying {} code with {} rules", request.getLanguage(), 
                request.getRules() != null ? request.getRules().size() : 0);

        try {
            ParserService parser = parserFactory.getParser(request.getLanguage());
            VerificationResult result = parser.verifyCode(request.getCode(), request.getRules());

            log.info("Verification completed in {}ms. Passed: {}, Violations: {}", 
                    result.getParsingTimeMs(), result.isPassed(), 
                    result.getViolations() != null ? result.getViolations().size() : 0);

            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.error("Invalid language: {}", request.getLanguage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check syntax errors only
     * 
     * POST /api/parser/check-syntax
     */
    @PostMapping("/check-syntax")
    @PreAuthorize("hasAnyRole('STUDENT', 'MODERATOR', 'ADMIN')")
    public ResponseEntity<SyntaxCheckResponse> checkSyntax(@Valid @RequestBody SyntaxCheckRequest request) {
        try {
            ParserService parser = parserFactory.getParser(request.getLanguage());
            boolean hasErrors = parser.hasSyntaxErrors(request.getCode());

            return ResponseEntity.ok(new SyntaxCheckResponse(
                hasErrors,
                hasErrors ? "Code contains syntax errors" : "No syntax errors found"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get supported languages
     * 
     * GET /api/parser/languages
     */
    @GetMapping("/languages")
    public ResponseEntity<List<String>> getSupportedLanguages() {
        return ResponseEntity.ok(List.of("JAVA", "PYTHON"));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyRequest {
        private String code;
        private String language;
        private List<VerificationRule> rules;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyntaxCheckRequest {
        private String code;
        private String language;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyntaxCheckResponse {
        private boolean hasErrors;
        private String message;
    }
}
