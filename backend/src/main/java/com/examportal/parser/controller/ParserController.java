package com.examportal.parser.controller;

import com.examportal.parser.model.VerificationResult;
import com.examportal.parser.model.VerificationRule;
import com.examportal.parser.service.ParserFactory;
import com.examportal.parser.service.ParserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ParserController {

    private static final Logger log = LoggerFactory.getLogger(ParserController.class);

    private final ParserFactory parserFactory;

    public ParserController(ParserFactory parserFactory) {
        this.parserFactory = parserFactory;
    }

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

    public static class VerifyRequest {
        private String code;
        private String language;
        private List<VerificationRule> rules;

        public VerifyRequest() {}
        public VerifyRequest(String code, String language, List<VerificationRule> rules) {
            this.code = code;
            this.language = language;
            this.rules = rules;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public List<VerificationRule> getRules() { return rules; }
        public void setRules(List<VerificationRule> rules) { this.rules = rules; }
    }

    public static class SyntaxCheckRequest {
        private String code;
        private String language;

        public SyntaxCheckRequest() {}
        public SyntaxCheckRequest(String code, String language) {
            this.code = code;
            this.language = language;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }

    public static class SyntaxCheckResponse {
        private boolean hasErrors;
        private String message;

        public SyntaxCheckResponse() {}
        public SyntaxCheckResponse(boolean hasErrors, String message) {
            this.hasErrors = hasErrors;
            this.message = message;
        }

        public boolean isHasErrors() { return hasErrors; }
        public void setHasErrors(boolean hasErrors) { this.hasErrors = hasErrors; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
