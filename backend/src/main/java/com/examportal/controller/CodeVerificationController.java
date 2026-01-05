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
    public ResponseEntity<CodeVerificationResult> verifyCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String language = request.get("language");

        log.info("Verifying code for language: {}", language);
        
        CodeVerificationResult result = codeVerificationService.verifyCode(code, language);
        
        return ResponseEntity.ok(result);
    }
}
