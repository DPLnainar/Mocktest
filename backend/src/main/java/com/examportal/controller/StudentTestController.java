package com.examportal.controller;

import com.examportal.dto.AnswerSubmissionDTO;
import com.examportal.dto.QueuePositionResponse;
import com.examportal.dto.TestDTO;
import com.examportal.entity.StudentAttempt;
import com.examportal.exception.RateLimitExceededException;
import com.examportal.execution.model.ExecutionResult;
import com.examportal.service.QueueMetricsService;
import com.examportal.service.RateLimitService;
import com.examportal.service.TestAttemptService;
import com.examportal.service.TestService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('STUDENT')")
public class StudentTestController {

    private final TestService testService;
    private final TestAttemptService attemptService;
    private final RateLimitService rateLimitService;
    private final QueueMetricsService queueMetricsService;

    @GetMapping("/tests")
    public ResponseEntity<List<TestDTO>> getAvailableTests() {
        List<TestDTO> tests = testService.getAvailableTestsForStudent();
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/tests/{testId}")
    public ResponseEntity<TestDTO> getTest(@PathVariable Long testId) {
        TestDTO test = testService.getTestById(testId);
        return ResponseEntity.ok(test);
    }

    @PostMapping("/tests/{testId}/start")
    public ResponseEntity<StudentAttempt> startTest(@PathVariable Long testId) {
        System.out.println("START TEST: Request received for testId " + testId);
        StudentAttempt attempt = attemptService.startTest(testId);
        System.out.println("START TEST: Attempt created successfully for attemptId " + attempt.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(attempt);
    }

    @GetMapping("/tests/{testId}/attempt")
    public ResponseEntity<StudentAttempt> getAttempt(@PathVariable Long testId) {
        StudentAttempt attempt = attemptService.getAttemptForTest(testId);
        if (attempt == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(attempt);
    }

    @PostMapping("/attempts/{attemptId}/answer")
    public ResponseEntity<Void> submitAnswer(
            @PathVariable Long attemptId,
            @Valid @RequestBody AnswerSubmissionDTO dto) {
        attemptService.submitAnswer(attemptId, dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/attempts/{attemptId}/execute")
    public ResponseEntity<ExecutionResult> executeCode(
            @PathVariable Long attemptId,
            @RequestBody CodeExecutionRequest request) {

        // Get current student ID from security context
        Long studentId = attemptService.getAttemptForTest(attemptId).getStudentId();

        // Rate limit check
        if (!rateLimitService.allowRequest(studentId)) {
            long retryAfter = rateLimitService.getRetryAfterSeconds(studentId);
            throw new RateLimitExceededException(
                    "Too many submissions. Please wait " + retryAfter + " seconds.",
                    retryAfter);
        }

        ExecutionResult result = attemptService.executeCode(
                attemptId,
                request.getQuestionId(),
                request.getCode(),
                request.getLanguageId(),
                request.getStdin());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/attempts/{attemptId}/queue-position")
    public ResponseEntity<QueuePositionResponse> getQueuePosition(@PathVariable Long attemptId) {
        QueuePositionResponse position = queueMetricsService.getQueuePosition(attemptId);
        return ResponseEntity.ok(position);
    }

    @GetMapping("/attempts/{attemptId}/questions/{questionId}/result")
    public ResponseEntity<ExecutionResult> getExecutionResult(
            @PathVariable Long attemptId,
            @PathVariable Long questionId) {
        ExecutionResult result = attemptService.getExecutionResult(attemptId, questionId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<StudentAttempt> submitTest(@PathVariable Long attemptId) {
        StudentAttempt attempt = attemptService.submitTest(attemptId);
        return ResponseEntity.ok(attempt);
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<StudentAttempt> getAttemptById(@PathVariable Long attemptId) {
        StudentAttempt attempt = attemptService.getAttemptById(attemptId);
        return ResponseEntity.ok(attempt);
    }

    @Data
    public static class CodeExecutionRequest {
        private Long questionId;
        private String code;
        private Integer languageId;
        private String stdin;
    }
}
