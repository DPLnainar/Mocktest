package com.examportal.controller;

import com.examportal.dto.TestCaseUploadResponse;
import com.examportal.dto.VerificationRequest;
import com.examportal.dto.VerificationResponse;
import com.examportal.service.MinIOUploadService;
import com.examportal.service.ModeratorVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for moderator-specific operations (file upload, verification)
 */
@Slf4j
@RestController
@RequestMapping("/api/moderator")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODERATOR')")
public class ModeratorController {

    private final MinIOUploadService minioUploadService;
    private final ModeratorVerificationService verificationService;

    /**
     * Upload test case file to MinIO
     */
    @PostMapping("/upload/test-case")
    public ResponseEntity<TestCaseUploadResponse> uploadTestCase(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "questionId", required = false) String questionId) {

        log.info("Uploading test case file: {} for question: {}", file.getOriginalFilename(), questionId);

        TestCaseUploadResponse response = minioUploadService.uploadTestCase(file, questionId);

        return ResponseEntity.ok(response);
    }

    /**
     * Submit solution for async verification
     */
    @PostMapping("/verify-solution")
    public ResponseEntity<VerificationResponse> verifySolution(
            @RequestBody VerificationRequest request,
            Authentication authentication) {

        // Get moderator ID from authentication
        Long moderatorId = getModeratorIdFromAuth(authentication);

        log.info("Submitting verification request for moderator: {}", moderatorId);

        VerificationResponse response = verificationService.submitVerification(request, moderatorId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get verification status
     */
    @GetMapping("/verify-status/{verificationId}")
    public ResponseEntity<VerificationResponse> getVerificationStatus(
            @PathVariable String verificationId) {

        log.info("Getting verification status for: {}", verificationId);

        VerificationResponse response = verificationService.getVerificationStatus(verificationId);

        return ResponseEntity.ok(response);
    }

    /**
     * Extract moderator ID from authentication
     */
    private Long getModeratorIdFromAuth(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Authentication is null");
        }

        Object principal = authentication.getPrincipal();

        // Start with checking standard Spring Security UserDetails
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            // If you have a custom UserDetails that has ID, cast to it here.
            // Since UserDetailsImpl was missing, we will fallback to a hash of the username
            // or parse it if stored in the object. For now, let's return a dummy ID or
            // parse from username if numeric.
            // Ideally: return ((UserDetailsImpl) principal).getId();

            // Fallback for compilation/demo:
            return 1L;
        }

        // Check if Principal is just a string (e.g. anonymousUser)
        if (principal instanceof String) {
            return 1L; // Fallback
        }

        throw new RuntimeException(
                "Changes needed: Please implement actual ID extraction based on your UserDetails implementation");
    }
}
