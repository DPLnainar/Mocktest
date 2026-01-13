package com.examportal.service;

import com.examportal.config.RabbitMQConfig;
import com.examportal.dto.SubmissionMessage;
import com.examportal.entity.FailedSubmission;
import com.examportal.repository.FailedSubmissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to handle messages that failed after max retries and went to DLQ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final FailedSubmissionRepository failedSubmissionRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.DLQ)
    public void handleDeadLetter(Message message) {
        try {
            log.error("Message sent to DLQ: {}", new String(message.getBody()));

            // Parse the message
            SubmissionMessage submissionMessage = objectMapper.readValue(
                    message.getBody(),
                    SubmissionMessage.class);

            // Extract retry count from headers
            Integer retryCount = (Integer) message.getMessageProperties()
                    .getHeaders()
                    .getOrDefault("x-death-count", 0);

            // Get error details from headers
            String errorMessage = (String) message.getMessageProperties()
                    .getHeaders()
                    .getOrDefault("x-first-death-reason", "Unknown error");

            // Store in database
            FailedSubmission failedSubmission = FailedSubmission.builder()
                    .executionId(submissionMessage.getExecutionId())
                    .attemptId(submissionMessage.getAttemptId())
                    .questionId(submissionMessage.getQuestionId())
                    .studentId(submissionMessage.getStudentId())
                    .errorMessage(errorMessage)
                    .retryCount(retryCount)
                    .originalMessage(convertToMap(submissionMessage))
                    .build();

            failedSubmissionRepository.save(java.util.Objects.requireNonNull(failedSubmission));

            log.error("Failed submission stored in database. ExecutionId: {}, Retries: {}",
                    submissionMessage.getExecutionId(), retryCount);

        } catch (Exception e) {
            log.error("Error processing dead letter message", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(SubmissionMessage message) {
        try {
            return objectMapper.convertValue(message, Map.class);
        } catch (Exception e) {
            log.error("Error converting message to map", e);
            return new HashMap<>();
        }
    }

}
