package com.examportal.service;

import com.examportal.dto.AnswerSubmissionDTO;
import com.examportal.entity.*;
import com.examportal.execution.service.Judge0Service;
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
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestAttemptService {

    private final StudentAttemptRepository attemptRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final DepartmentSecurityService departmentSecurityService;
    private final Judge0Service judge0Service;

    @Transactional
    public StudentAttempt startTest(Long testId) {
        Long studentId = departmentSecurityService.getCurrentUserId();
        
        // Check if already attempted
        if (attemptRepository.existsByTestIdAndStudentId(testId, studentId)) {
            throw new RuntimeException("You have already started this test");
        }
        
        Test test = testRepository.findById(testId)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        // Verify department access
        String studentDepartment = departmentSecurityService.getCurrentUserDepartment();
        if (!test.getDepartment().equals(studentDepartment)) {
            throw new SecurityException("Access denied: Test not available for your department");
        }
        
        // Verify test is active
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(test.getStartDateTime())) {
            throw new RuntimeException("Test has not started yet");
        }
        if (now.isAfter(test.getEndDateTime())) {
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
            .totalMarks(test.getQuestions().stream()
                .mapToInt(Question::getMarks)
                .sum() * 1.0)
            .build();
        
        return attemptRepository.save(attempt);
    }

    @Transactional
    public void submitAnswer(Long attemptId, AnswerSubmissionDTO dto) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
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
    public ExecutionResult executeCode(Long attemptId, Long questionId, String code, String stdin) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        // Verify ownership
        Long studentId = departmentSecurityService.getCurrentUserId();
        if (!attempt.getStudentId().equals(studentId)) {
            throw new SecurityException("Access denied");
        }
        
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("Question not found"));
        
        if (question.getType() != QuestionType.CODING) {
            throw new RuntimeException("Question is not a coding question");
        }
        
        // Execute code via Judge0
        ExecutionResult result = judge0Service.executeCode(
            code, 
            question.getLanguageId(), 
            stdin != null ? stdin : "", 
            studentId
        );
        
        // Store execution result (don't override answer, just save for reference)
        Map<String, Object> results = attempt.getExecutionResults();
        results.put(questionId.toString(), result);
        attemptRepository.save(attempt);
        
        return result;
    }

    @Transactional
    public StudentAttempt submitTest(Long attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        // Verify ownership
        Long studentId = departmentSecurityService.getCurrentUserId();
        if (!attempt.getStudentId().equals(studentId)) {
            throw new SecurityException("Access denied");
        }
        
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new RuntimeException("Test already submitted");
        }
        
        Test test = testRepository.findById(attempt.getTestId())
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        // Server-side time validation (Phase 2: Anti-cheat)
        LocalDateTime actualEnd = LocalDateTime.now();
        attempt.setActualEndTime(actualEnd);
        
        if (attempt.getActualStartTime() != null) {
            long actualDurationMinutes = java.time.Duration.between(
                attempt.getActualStartTime(), 
                actualEnd
            ).toMinutes();
            
            // Allow 5-minute buffer for network delays
            long maxAllowedMinutes = test.getDurationMinutes() + 5;
            
            if (actualDurationMinutes > maxAllowedMinutes) {
                log.warn("Time limit exceeded for attempt {}: {} minutes (max {})", 
                    attemptId, actualDurationMinutes, maxAllowedMinutes);
                throw new RuntimeException(
                    "Time limit exceeded. Actual duration: " + actualDurationMinutes + 
                    " minutes, allowed: " + maxAllowedMinutes + " minutes"
                );
            }
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
        
        for (Question question : test.getQuestions()) {
            if (question.getType() == QuestionType.MCQ) {
                String studentAnswer = attempt.getAnswers().get(question.getId().toString());
                if (studentAnswer != null && studentAnswer.equals(question.getCorrectOption())) {
                    score += question.getMarks();
                }
            }
            // Coding questions are not auto-graded, require manual review
        }
        
        return score;
    }

    public StudentAttempt getAttemptById(Long attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        // Verify ownership or moderator access
        Long userId = departmentSecurityService.getCurrentUserId();
        if (!attempt.getStudentId().equals(userId) && 
            !departmentSecurityService.isCurrentUserAdmin()) {
            
            // Check if current user is moderator of the test's department
            Test test = testRepository.findById(attempt.getTestId())
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
}
