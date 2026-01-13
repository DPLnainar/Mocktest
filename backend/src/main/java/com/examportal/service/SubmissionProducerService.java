package com.examportal.service;

import com.examportal.config.RabbitMQConfig;
import com.examportal.dto.SubmissionMessage;
import com.examportal.entity.StudentAttempt;
import com.examportal.execution.model.ExecutionResult;
import com.examportal.repository.StudentAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionProducerService {

        private final RabbitTemplate rabbitTemplate;
        private final StudentAttemptRepository attemptRepository;

        @Transactional
        public String queueSubmission(Long attemptId, Long questionId, Long studentId,
                        String code, Integer languageId, String stdin,
                        Map<String, Boolean> constraints) {

                if (attemptId == null) {
                        throw new IllegalArgumentException("Attempt ID cannot be null");
                }

                String executionId = UUID.randomUUID().toString();
                log.info("Queuing submission {} for attempt {} question {}", executionId, attemptId, questionId);

                // 1. Create Initial ExecutionResult (QUEUED)
                ExecutionResult initialResult = ExecutionResult.builder()
                                .executionId(executionId)
                                .status(ExecutionResult.ExecutionStatus.QUEUED)
                                .executedAt(LocalDateTime.now())
                                .build();

                // 2. Save to Database (Optimistic locking might be needed, but Map updates are
                // tricky in JPA)
                // We fetch the attempt ensure we have the latest
                StudentAttempt attempt = attemptRepository.findById(attemptId)
                                .orElseThrow(() -> new RuntimeException("Attempt not found during queuing"));

                attempt.getExecutionResults().put(questionId.toString(), initialResult);
                attemptRepository.save(attempt);

                // 3. Build Message
                SubmissionMessage message = SubmissionMessage.builder()
                                .executionId(executionId)
                                .attemptId(attemptId)
                                .questionId(questionId)
                                .studentId(studentId)
                                .code(code)
                                .languageId(languageId)
                                .stdin(stdin)
                                .constraints(constraints)
                                .build();

                // 4. Publish to RabbitMQ
                try {
                        rabbitTemplate.convertAndSend(
                                        RabbitMQConfig.EXCHANGE,
                                        RabbitMQConfig.ROUTING_KEY,
                                        message);
                        log.info("Published submission {} to RabbitMQ", executionId);
                } catch (Exception e) {
                        log.error("Failed to publish submission to RabbitMQ", e);
                        // Revert DB status to INTERNAL_ERROR if queue fails
                        initialResult.setStatus(ExecutionResult.ExecutionStatus.INTERNAL_ERROR);
                        initialResult.setError("Submission Failed: Queue unavailable");
                        attempt.getExecutionResults().put(questionId.toString(), initialResult);
                        attemptRepository.save(attempt);
                        throw new RuntimeException("Failed to queue submission");
                }

                return executionId;
        }
}
