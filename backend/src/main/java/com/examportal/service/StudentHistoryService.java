package com.examportal.service;

import com.examportal.entity.*;
import com.examportal.repository.StudentAttemptRepository;
import com.examportal.repository.TestRepository;

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

    private final DepartmentSecurityService departmentSecurityService;

    public List<Map<String, Object>> getStudentHistory() {
        Long studentId = departmentSecurityService.getCurrentUserId();
        List<StudentAttempt> attempts = attemptRepository.findByStudentIdOrderByStartedAtDesc(studentId);

        // Fetch test titles efficiently
        List<Long> testIds = attempts.stream().map(StudentAttempt::getTestId).filter(java.util.Objects::nonNull)
                .distinct().toList();
        Map<Long, String> testTitles = new HashMap<>();
        testRepository.findAllById(java.util.Objects.requireNonNull(testIds))
                .forEach(test -> testTitles.put(test.getId(), test.getTitle()));

        return attempts.stream().map(attempt -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", attempt.getId());
            map.put("testId", attempt.getTestId());
            map.put("testTitle", testTitles.getOrDefault(attempt.getTestId(), "Unknown Test"));
            map.put("status", attempt.getStatus());
            map.put("startedAt", attempt.getStartedAt());
            map.put("submittedAt", attempt.getSubmittedAt());
            map.put("score", attempt.getScore());
            map.put("totalMarks", attempt.getTotalMarks());
            map.put("violationCount", attempt.getViolationCount());
            map.put("autoSubmitted", attempt.getAutoSubmitted());
            return map;
        }).toList();
    }

    public Map<String, Object> getTestReview(Long testId) {
        Long studentId = departmentSecurityService.getCurrentUserId();

        // Get test
        Test test = testRepository.findById(java.util.Objects.requireNonNull(testId))
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Get student's attempt
        StudentAttempt attempt = attemptRepository.findByTestIdAndStudentId(testId, studentId)
                .orElseThrow(() -> new RuntimeException("No attempt found for this test"));

        // Verify access: Allow review if deadline passed OR if attempt is
        // submitted/frozen
        boolean isCompleted = attempt.getStatus() == AttemptStatus.SUBMITTED ||
                attempt.getStatus() == AttemptStatus.FROZEN;

        if (!isCompleted && LocalDateTime.now().isBefore(test.getEndDateTime())) {
            throw new RuntimeException("Review available only after test deadline or upon submission");
        }

        // Build review data
        Map<String, Object> review = new HashMap<>();
        review.put("test", test);
        review.put("attempt", attempt);
        review.put("questions", buildQuestionReview(test, attempt));

        return review;
    }

    private List<Map<String, Object>> buildQuestionReview(Test test, StudentAttempt attempt) {
        return test.getTestQuestions().stream()
                .map(tq -> {
                    Question question = tq.getQuestion();
                    Map<String, Object> qReview = new HashMap<>();
                    qReview.put("question", question);
                    qReview.put("marks", tq.getMarks());
                    qReview.put("sectionName", tq.getSectionName());

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
    }
}
