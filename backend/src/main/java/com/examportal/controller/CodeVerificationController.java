package com.examportal.controller;

import com.examportal.dto.CodeVerificationResult;
import com.examportal.service.CodeVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeVerificationController {

    private final CodeVerificationService codeVerificationService;

    @PostMapping("/verify")
    public ResponseEntity<CodeVerificationResult> verifyCode(@RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        String language = (String) request.get("language");

        // Extract constraints safely
        @SuppressWarnings("unchecked")
        Map<String, Boolean> constraints = (Map<String, Boolean>) request.get("constraints");
        if (constraints == null) {
            constraints = java.util.Collections.emptyMap();
        }

        // Extract allowedLanguageIds safely
        @SuppressWarnings("unchecked")
        java.util.List<Integer> allowedLanguageIds = (java.util.List<Integer>) request.get("allowedLanguageIds");

        log.info("Verifying code for language: {}", language);

        CodeVerificationResult result = codeVerificationService.verifyCode(code, language, constraints,
                allowedLanguageIds);

        return ResponseEntity.ok(result);
    }
}
