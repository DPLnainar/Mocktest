package com.examportal.service;

import com.examportal.dto.StudentResultDTO;
import com.examportal.entity.*;
import com.examportal.repository.StudentAttemptRepository;
import com.examportal.repository.TestRepository;
import com.examportal.repository.QuestionRepository;
import com.examportal.security.DepartmentSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentHistoryService {

    private final StudentAttemptRepository attemptRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final DepartmentSecurityService departmentSecurityService;

    public List<StudentAttempt> getStudentHistory() {
        Long studentId = departmentSecurityService.getCurrentUserId();
        return attemptRepository.findByStudentIdOrderByStartedAtDesc(studentId);
    }

    public Map<String, Object> getTestReview(Long testId) {
        Long studentId = departmentSecurityService.getCurrentUserId();
        
        // Get test
        Test test = testRepository.findById(testId)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        // Verify deadline has passed
        if (LocalDateTime.now().isBefore(test.getEndDateTime())) {
            throw new RuntimeException("Review available only after test deadline");
        }
        
        // Get student's attempt
        StudentAttempt attempt = attemptRepository.findByTestIdAndStudentId(testId, studentId)
            .orElseThrow(() -> new RuntimeException("No attempt found for this test"));
        
        // Build review data
        Map<String, Object> review = new HashMap<>();
        review.put("test", test);
        review.put("attempt", attempt);
        review.put("questions", buildQuestionReview(test, attempt));
        
        return review;
    }

    private List<Map<String, Object>> buildQuestionReview(Test test, StudentAttempt attempt) {
        List<Map<String, Object>> questionReviews = test.getQuestions().stream()
            .map(question -> {
                Map<String, Object> qReview = new HashMap<>();
                qReview.put("question", question);
                
                String studentAnswer = attempt.getAnswers().get(question.getId().toString());
                qReview.put("studentAnswer", studentAnswer);
                
                // For MCQ, show if correct
                if (question.getType() == QuestionType.MCQ) {
                    boolean isCorrect = question.getCorrectOption().equals(studentAnswer);
                    qReview.put("correct", isCorrect);
                    qReview.put("correctAnswer", question.getCorrectOption());
                }
                
                // For coding, show execution result
                if (question.getType() == QuestionType.CODING) {
                    Object executionResult = attempt.getExecutionResults()
                        .get(question.getId().toString());
                    qReview.put("executionResult", executionResult);
                }
                
                qReview.put("explanation", question.getExplanation());
                
                return qReview;
            })
            .toList();
        
        return questionReviews;
    }
}
