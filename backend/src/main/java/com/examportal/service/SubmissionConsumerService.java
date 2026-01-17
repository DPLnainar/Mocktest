package com.examportal.service;

import com.examportal.config.RabbitMQConfig;
import com.examportal.dto.SubmissionMessage;
import com.examportal.entity.Question;
import com.examportal.entity.StudentAttempt;
import com.examportal.execution.model.ExecutionResult;
import com.examportal.repository.QuestionRepository;
import com.examportal.repository.StudentAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionConsumerService {

    private final SubmissionExecutionService submissionExecutionService;
    private final StudentAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final StorageService storageService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    @Transactional
    public void consumeSubmission(SubmissionMessage message) {
        log.info("Processing submission {} for attempt {}", message.getExecutionId(), message.getAttemptId());

        if (message.getAttemptId() == null || message.getQuestionId() == null || message.getStudentId() == null) {
            log.error("Invalid submission message: missing fields. {}", message);
            return;
        }

        try {
            Question question = questionRepository.findById(Objects.requireNonNull(message.getQuestionId()))
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            // FETCH TEST CASES logic
            String testCasesJson = null;
            if ("S3".equalsIgnoreCase(question.getStorageType())) {
                log.info("Fetching test cases from S3 for question {}", question.getId());
                testCasesJson = storageService.getTestCases(question.getId().toString());
                log.debug("Fetched {} bytes of test cases from S3", testCasesJson != null ? testCasesJson.length() : 0);
            }

            // 2. Execute Code
            ExecutionResult result = submissionExecutionService.executeSubmission(
                    message.getExecutionId(),
                    message.getCode(),
                    message.getLanguageId(),
                    message.getConstraints(),
                    Objects.requireNonNull(message.getStudentId()),
                    message.getAttemptId(),
                    message.getQuestionId());

            // 3. Update Result in DB
            StudentAttempt attempt = attemptRepository.findById(Objects.requireNonNull(message.getAttemptId()))
                    .orElseThrow(() -> new RuntimeException("Attempt not found"));

            // Reuse the executionId from the message for consistency
            result.setExecutionId(message.getExecutionId());

            Map<String, Object> results = attempt.getExecutionResults();
            results.put(message.getQuestionId().toString(), result);
            attemptRepository.save(attempt);

            log.info("Submission {} processed with status {}", message.getExecutionId(), result.getStatus());

        } catch (Exception e) {
            log.error("Error processing submission {}", message.getExecutionId(), e);
            // Update DB with Error
            StudentAttempt attempt = attemptRepository.findById(Objects.requireNonNull(message.getAttemptId()))
                    .orElse(null);
            if (attempt != null) {
                ExecutionResult errorResult = ExecutionResult.builder()
                        .executionId(message.getExecutionId())
                        .status(ExecutionResult.ExecutionStatus.INTERNAL_ERROR)
                        .error("Async Execution Failed: " + e.getMessage())
                        .build();
                attempt.getExecutionResults().put(message.getQuestionId().toString(), errorResult);
                attemptRepository.save(attempt);
            }
        }
    }
}
