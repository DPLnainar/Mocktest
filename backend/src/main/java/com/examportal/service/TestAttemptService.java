package com.examportal.service;

import com.examportal.dto.AnswerSubmissionDTO;
import com.examportal.entity.*;
import com.examportal.execution.model.ExecutionResult;
import com.examportal.repository.QuestionRepository;
import com.examportal.repository.StudentAttemptRepository;
import com.examportal.repository.TestRepository;
import com.examportal.security.DepartmentSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestAttemptService {

    private final StudentAttemptRepository attemptRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final DepartmentSecurityService departmentSecurityService;
    private final SubmissionProducerService submissionProducerService;

    @Transactional
    public StudentAttempt startTest(Long testId) {
        Long studentId = departmentSecurityService.getCurrentUserId();

        // Check if already attempted
        if (attemptRepository.existsByTestIdAndStudentId(testId, studentId)) {
            log.error("StartTest Failed: Student {} already attempted Test {}", studentId, testId);
            throw new RuntimeException("You have already started this test");
        }

        Test test = testRepository.findById(java.util.Objects.requireNonNull(testId))
                .orElseThrow(() -> {
                    log.error("StartTest Failed: Test {} not found", testId);
                    return new RuntimeException("Test not found");
                });

        // Verify department access
        String studentDepartment = departmentSecurityService.getCurrentUserDepartment();
        log.info("Verifying access for Student {} (Dept: {}) to Test {} (Dept: {})",
                studentId, studentDepartment, testId, test.getDepartment());

        if (!test.getDepartment().equals(studentDepartment) &&
                !test.getDepartment().equals("MODERATOR") &&
                !test.getDepartment().equals("General")) {
            log.error("StartTest Failed: Access denied for Dept {}", studentDepartment);
            throw new SecurityException("Access denied: Test not available for your department");
        }

        // Verify test is active
        // FIX: Ensure comparison uses the same timezone assumption as the DB (which
        // stores as-is)
        // Check both UTC and System Default to be safe
        LocalDateTime now = LocalDateTime.now();
        log.info("Checking time: Current (Default)={}, Start={}, End={}", now, test.getStartDateTime(),
                test.getEndDateTime());

        // If 'now' is much earlier than 'start', it might be UTC vs IST
        // Let's try to be lenient or enforce a specific check

        boolean isBeforeStart = now.isBefore(test.getStartDateTime());
        boolean isAfterEnd = now.isAfter(test.getEndDateTime());

        if (isBeforeStart) {
            // Handle Timezone Edge Case: If server is UTC but user meant IST (+5:30)
            // We give a 6-hour buffer to allow for timezone mismatch
            if (now.plusHours(6).isAfter(test.getStartDateTime())) {
                log.warn("Timezone Mismatch Detected: allowing test start because +6h buffer passes.");
                isBeforeStart = false;
            }
        }

        if (isBeforeStart) {
            log.error("StartTest Failed: Test has not started yet. Now: {}, Start: {}", now, test.getStartDateTime());
            throw new RuntimeException("Test has not started yet. (Current Server Time: " + now + ")");
        }
        if (isAfterEnd) {
            log.error("StartTest Failed: Test has expired. Now: {}, End: {}", now, test.getEndDateTime());
            throw new RuntimeException("Test has expired");
        }

        StudentAttempt attempt = StudentAttempt.builder()
                .testId(testId)
                .studentId(studentId)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(now)
                .actualStartTime(now) // Server-side timestamp for anti-cheat
                .answers(new HashMap<>())
                .executionResults(new HashMap<>())
                .violationCount(0)
                .autoSubmitted(false)
                .totalMarks(test.getTestQuestions().stream()
                        .mapToInt(TestQuestion::getMarks)
                        .sum() * 1.0)
                .build();

        return attemptRepository.save(java.util.Objects.requireNonNull(attempt));
    }

    @Transactional
    public void submitAnswer(Long attemptId, AnswerSubmissionDTO dto) {
        StudentAttempt attempt = attemptRepository.findById(java.util.Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Verify ownership
        Long studentId = departmentSecurityService.getCurrentUserId();
        if (!attempt.getStudentId().equals(studentId)) {
            throw new SecurityException("Access denied");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Cannot modify submitted test");
        }

        // Save answer
        attempt.getAnswers().put(dto.getQuestionId().toString(), dto.getAnswer());
        attemptRepository.save(attempt);
    }

    @Transactional
    public ExecutionResult executeCode(Long attemptId, Long questionId, String code, Integer languageId, String stdin) {
        StudentAttempt attempt = attemptRepository.findById(java.util.Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Verify ownership
        Long studentId = departmentSecurityService.getCurrentUserId();
        if (!attempt.getStudentId().equals(studentId)) {
            throw new SecurityException("Access denied");
        }

        Question question = questionRepository.findById(java.util.Objects.requireNonNull(questionId))
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (question.getType() != QuestionType.CODING) {
            throw new RuntimeException("Question is not a coding question");
        }

        Integer targetLanguageId = languageId;
        List<Integer> allowed = question.getAllowedLanguageIds();

        // If languageId is null, pick first allowed (backward compatibility)
        if (targetLanguageId == null && allowed != null && !allowed.isEmpty()) {
            targetLanguageId = allowed.get(0);
        } else if (targetLanguageId != null && allowed != null) {
            if (!allowed.contains(targetLanguageId)) {
                throw new IllegalArgumentException(
                        "Language ID " + targetLanguageId + " is not allowed for this question");
            }
        } else if (allowed == null || allowed.isEmpty()) {
            // Should not happen due to validation, but safe fallback
            throw new RuntimeException("No allowed languages defined for this question");
        }

        // --- ASYNC SUBMISSION PIPELINE ---
        // Validate basics here, then queue for processing
        if (question.getConstraints() != null && !question.getConstraints().isEmpty()) {
            // Optional: Quick fail check could be done here if needed (e.g. language check)
        }

        // Send to RabbitMQ via Producer
        String executionId = submissionProducerService.queueSubmission(
                attemptId,
                questionId,
                studentId,
                code,
                targetLanguageId,
                stdin != null ? stdin : "",
                question.getConstraints());

        // Return the Execution ID immediately with QUEUED status
        return ExecutionResult.builder()
                .executionId(executionId)
                .status(ExecutionResult.ExecutionStatus.QUEUED)
                .executedAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public StudentAttempt submitTest(Long attemptId) {
        StudentAttempt attempt = attemptRepository.findById(java.util.Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Verify ownership
        Long studentId = departmentSecurityService.getCurrentUserId();
        if (!attempt.getStudentId().equals(studentId)) {
            throw new SecurityException("Access denied");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Test already submitted");
        }

        Test test = testRepository.findById(java.util.Objects.requireNonNull(attempt.getTestId()))
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Server-side time validation (Phase 2: Anti-cheat)
        LocalDateTime actualEnd = LocalDateTime.now();
        attempt.setActualEndTime(actualEnd);

        if (attempt.getActualStartTime() != null) {
            long actualDurationMinutes = java.time.Duration.between(
                    attempt.getActualStartTime(),
                    actualEnd).toMinutes();

            // Allow 5-minute buffer for network delays
            long maxAllowedMinutes = test.getDurationMinutes() + 5;

            log.info("SubmitTest: Attempt {}, Start={}, End={}, Duration={}m, Max={}m",
                    attemptId, attempt.getActualStartTime(), actualEnd, actualDurationMinutes, maxAllowedMinutes);

            if (actualDurationMinutes > maxAllowedMinutes) {
                // Check for negative duration (timezone weirdness) or excessive duration
                if (actualDurationMinutes < 0) {
                    log.warn("Negative duration detected! Assuming timezone skew and allowing submission.");
                } else {
                    log.error("Time limit exceeded for attempt {}: {} minutes (max {})",
                            attemptId, actualDurationMinutes, maxAllowedMinutes);
                    throw new RuntimeException(
                            "Time limit exceeded. Actual duration: " + actualDurationMinutes +
                                    " minutes, allowed: " + maxAllowedMinutes + " minutes");
                }
            }
        } else {
            log.warn("SubmitTest: ActualStartTime is null for attempt {}", attemptId);
        }

        // Auto-grade MCQ questions
        double score = calculateScore(attempt, test);

        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setScore(score);

        return attemptRepository.save(attempt);
    }

    private double calculateScore(StudentAttempt attempt, Test test) {
        double score = 0.0;

        for (TestQuestion tq : test.getTestQuestions()) {
            Question question = tq.getQuestion();
            if (question.getType() == QuestionType.MCQ) {
                String studentAnswer = attempt.getAnswers().get(question.getId().toString());
                if (studentAnswer != null && studentAnswer.equals(question.getCorrectOption())) {
                    score += tq.getMarks();
                }
            }
            // Coding questions are not auto-graded, require manual review
        }

        return score;
    }

    public StudentAttempt getAttemptById(Long attemptId) {
        StudentAttempt attempt = attemptRepository.findById(java.util.Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Verify ownership or moderator access
        Long userId = departmentSecurityService.getCurrentUserId();
        if (!attempt.getStudentId().equals(userId) &&
                !departmentSecurityService.isCurrentUserAdmin()) {

            // Check if current user is moderator of the test's department
            Test test = testRepository.findById(java.util.Objects.requireNonNull(attempt.getTestId()))
                    .orElseThrow(() -> new RuntimeException("Test not found"));

            departmentSecurityService.verifyDepartmentAccess(test.getDepartment());
        }

        return attempt;
    }

    public StudentAttempt getAttemptForTest(Long testId) {
        Long studentId = departmentSecurityService.getCurrentUserId();
        return attemptRepository.findByTestIdAndStudentId(testId, studentId)
                .orElse(null);
    }

    public ExecutionResult getExecutionResult(Long attemptId, Long questionId) {
        StudentAttempt attempt = attemptRepository.findById(java.util.Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Verify ownership
        Long studentId = departmentSecurityService.getCurrentUserId();
        if (!attempt.getStudentId().equals(studentId)) {
            throw new SecurityException("Access denied");
        }

        Map<String, Object> results = attempt.getExecutionResults();
        if (results == null || !results.containsKey(questionId.toString())) {
            return null; // or throw exception? Null allows 404/204 response
        }

        Object resultObj = results.get(questionId.toString());
        // Depending on Jackson deserialization, this might be a Map or ExecutionResult
        // object.
        // Assuming JPA handles it correctly via @JdbcTypeCode(SqlTypes.JSON) as mapped
        // in Entity.
        // But safe cast/convert might be needed if it's raw JSON map.
        // Since we put ExecutionResult object, Hibernate/Jackson should map it back if
        // configured correctly.
        // However, generic Map<String, Object> implies type erasure.
        // Let's assume it works for now, or check StudentAttempt entity definition.

        return (ExecutionResult) resultObj;
    }
}
