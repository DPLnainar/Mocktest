package com.examportal.service;

import com.examportal.entity.Question;
import com.examportal.entity.QuestionType;
import com.examportal.entity.StudentAttempt;
import com.examportal.execution.model.ExecutionResult;
import com.examportal.repository.QuestionRepository;
import com.examportal.repository.StudentAttemptRepository;
import com.examportal.repository.TestRepository;
import com.examportal.security.DepartmentSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubmissionPipelineTest {

        @Mock
        private StudentAttemptRepository attemptRepository;
        @Mock
        private QuestionRepository questionRepository;
        @Mock
        private TestRepository testRepository;
        @Mock
        private DepartmentSecurityService departmentSecurityService;
        @Mock
        private SubmissionProducerService submissionProducerService;

        private TestAttemptService testAttemptService;

        @BeforeEach
        void setUp() {
                testAttemptService = new TestAttemptService(
                                attemptRepository,
                                testRepository,
                                questionRepository,
                                departmentSecurityService,
                                submissionProducerService);
        }

        @Test
        void testExecuteCode_QueuesSubmission() {
                // Arrange
                Long attemptId = 1L;
                Long questionId = 100L;
                Long studentId = 55L;
                String code = "public class Main { ... }";
                String executionId = "exec-123";

                Question question = Question.builder()
                                .id(questionId)
                                .type(QuestionType.CODING)
                                .allowedLanguageIds(List.of(62))
                                .constraints(Map.of())
                                .build();

                StudentAttempt attempt = StudentAttempt.builder()
                                .id(attemptId)
                                .studentId(studentId)
                                .status(com.examportal.entity.AttemptStatus.IN_PROGRESS)
                                .executionResults(new java.util.HashMap<>())
                                .build();

                when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
                when(questionRepository.findById(questionId)).thenReturn(Optional.of(question));
                when(departmentSecurityService.getCurrentUserId()).thenReturn(studentId);
                when(submissionProducerService.queueSubmission(any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(executionId);

                // Act
                ExecutionResult result = testAttemptService.executeCode(attemptId, questionId, code, 62, "");

                // Assert
                assertNotNull(result);
                assertEquals(ExecutionResult.ExecutionStatus.QUEUED, result.getStatus());
                assertEquals(executionId, result.getExecutionId());

                // Verify Producer was called
                verify(submissionProducerService, times(1)).queueSubmission(
                                eq(attemptId),
                                eq(questionId),
                                eq(studentId),
                                eq(code),
                                eq(62),
                                eq(""),
                                eq(question.getConstraints()));
        }
}
