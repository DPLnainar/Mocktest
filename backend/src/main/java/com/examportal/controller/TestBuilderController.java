package com.examportal.controller;

import com.examportal.dto.TestCaseUploadResponse;
import com.examportal.dto.VerificationRequest;
import com.examportal.dto.VerificationResponse;
import com.examportal.service.MinIOUploadService;
import com.examportal.service.ModeratorVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/moderator")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TestBuilderController {

    private final MinIOUploadService minioUploadService;
    private final ModeratorVerificationService moderatorVerificationService;

    @PostMapping("/upload/test-case")
    public ResponseEntity<TestCaseUploadResponse> uploadTestCase(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "questionId", required = false) String questionId) {

        log.info("Received test case upload request for question: {}", questionId);
        TestCaseUploadResponse response = minioUploadService.uploadTestCase(file, questionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-solution")
    public ResponseEntity<VerificationResponse> verifySolution(
            @RequestBody VerificationRequest request,
            @RequestParam("moderatorId") Long moderatorId) {

        log.info("Received verification request from moderator: {}", moderatorId);
        VerificationResponse response = moderatorVerificationService.submitVerification(request, moderatorId);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/verify-status/{verificationId}")
    public ResponseEntity<VerificationResponse> getVerificationStatus(
            @PathVariable String verificationId) {

        VerificationResponse response = moderatorVerificationService.getVerificationStatus(verificationId);
        return ResponseEntity.ok(response);
    }
}
