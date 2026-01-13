package com.examportal.service;

import com.examportal.dto.VerificationRequest;
import com.examportal.dto.VerificationResponse;
import com.examportal.dto.VerificationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling moderator solution verification requests
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeratorVerificationService {

    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String VERIFICATION_QUEUE = "verification_queue";
    private static final String REDIS_KEY_PREFIX = "verification:";
    private static final long REDIS_TTL_MINUTES = 30;

    /**
     * Submit verification request to queue
     */
    public VerificationResponse submitVerification(VerificationRequest request, Long moderatorId) {
        try {
            // Generate unique verification ID
            String verificationId = "ver_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            // Create initial response
            VerificationResponse response = VerificationResponse.builder()
                    .verificationId(verificationId)
                    .status(VerificationResponse.VerificationStatus.QUEUED)
                    .message("Solution verification queued")
                    .build();

            // Store in Redis
            String redisKey = REDIS_KEY_PREFIX + verificationId;
            redisTemplate.opsForValue().set(
                    redisKey,
                    objectMapper.writeValueAsString(response),
                    REDIS_TTL_MINUTES,
                    TimeUnit.MINUTES);

            // Create message for queue
            VerificationMessage message = VerificationMessage.builder()
                    .verificationId(verificationId)
                    .moderatorId(moderatorId)
                    .code(request.getCode())
                    .languageId(request.getLanguageId())
                    .testCases(request.getTestCases())
                    .constraints(request.getConstraints())
                    .build();

            // Push to RabbitMQ
            rabbitTemplate.convertAndSend(VERIFICATION_QUEUE, message);

            log.info("Submitted verification request: {} for moderator: {}", verificationId, moderatorId);

            return response;

        } catch (Exception e) {
            log.error("Failed to submit verification request", e);
            throw new RuntimeException("Failed to submit verification: " + e.getMessage(), e);
        }
    }

    /**
     * Get verification status from Redis
     */
    public VerificationResponse getVerificationStatus(String verificationId) {
        try {
            String redisKey = REDIS_KEY_PREFIX + verificationId;
            String json = redisTemplate.opsForValue().get(redisKey);

            if (json == null) {
                return VerificationResponse.builder()
                        .verificationId(verificationId)
                        .status(VerificationResponse.VerificationStatus.ERROR)
                        .message("Verification not found or expired")
                        .build();
            }

            return objectMapper.readValue(json, VerificationResponse.class);

        } catch (Exception e) {
            log.error("Failed to get verification status for: {}", verificationId, e);
            throw new RuntimeException("Failed to get verification status: " + e.getMessage(), e);
        }
    }

    /**
     * Update verification status in Redis (called by worker)
     */
    public void updateVerificationStatus(String verificationId, VerificationResponse.VerificationStatus status,
            VerificationResult result, String message) {
        try {
            String redisKey = REDIS_KEY_PREFIX + verificationId;

            VerificationResponse response = VerificationResponse.builder()
                    .verificationId(verificationId)
                    .status(status)
                    .message(message)
                    .result(result)
                    .build();

            redisTemplate.opsForValue().set(
                    redisKey,
                    objectMapper.writeValueAsString(response),
                    REDIS_TTL_MINUTES,
                    TimeUnit.MINUTES);

            log.info("Updated verification status: {} to {}", verificationId, status);

        } catch (Exception e) {
            log.error("Failed to update verification status for: {}", verificationId, e);
        }
    }

    /**
     * Message class for RabbitMQ
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VerificationMessage {
        private String verificationId;
        private Long moderatorId;
        private String code;
        private Integer languageId;
        private java.util.List<VerificationRequest.TestCaseData> testCases;
        private VerificationRequest.ConstraintsConfig constraints;
    }
}
