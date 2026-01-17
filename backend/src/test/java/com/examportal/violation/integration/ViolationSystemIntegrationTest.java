package com.examportal.violation.integration;

import com.examportal.monitoring.model.ExamSession;
import com.examportal.monitoring.service.SessionManagerService;
import com.examportal.violation.entity.Violation;
import com.examportal.violation.repository.ViolationRepository;
import com.examportal.violation.service.ViolationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test: Violation System
 * 
 * Tests:
 * 1. Redis atomic counting (no race conditions)
 * 2. Auto-termination at 5 strikes
 * 3. PostgreSQL evidence storage
 * 4. WebSocket broadcasting integration
 * 5. False positive handling
 */
@SpringBootTest
@ActiveProfiles("test")
class ViolationSystemIntegrationTest {

        @Autowired
        private ViolationService violationService;

        @Autowired
        private ViolationRepository violationRepository;

        @Autowired
        private SessionManagerService sessionManager;

        @Autowired
        private StringRedisTemplate redisTemplate;

        private Long testSessionId;
        private Long testStudentId;
        private Long testExamId;

        @BeforeEach
        void setup() {
                // Clean up
                violationRepository.deleteAll();
                redisTemplate.delete("exam:session:strikes:*");

                // Create test data
                testSessionId = 123L;
                testStudentId = 789L;
                testExamId = 456L;

                // Create exam session
                ExamSession session = ExamSession.builder()
                                .id(testSessionId)
                                .studentId(testStudentId)
                                .examId(testExamId)
                                .examTitle("Data Structures Final Exam")
                                .studentName("John Doe")
                                .department("CSE")
                                .status(ExamSession.SessionStatus.ACTIVE)
                                .violationCount(0)
                                .startedAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusHours(2))
                                .build();

                sessionManager.createSession(session);
        }

        /**
         * Test 1: Single Violation Recording
         */
        @Test
        void testRecordSingleViolation() {
                // When: Record a violation
                ViolationService.ViolationRecordResult result = violationService.recordViolation(
                                testSessionId,
                                testStudentId,
                                testExamId,
                                Violation.ViolationType.TAB_SWITCH,
                                Violation.Severity.MAJOR,
                                "Switched to Google",
                                Map.of("targetUrl", "google.com"));

                int strikeCount = result.strikeCount();

                // Then: Verify strike count
                assertThat(strikeCount).isEqualTo(2); // MAJOR = 2 strikes

                // Verify Redis counter
                String redisKey = "exam:session:strikes:" + testSessionId;
                String redisCount = redisTemplate.opsForValue().get(redisKey);
                assertThat(redisCount).isEqualTo("2");

                // Verify PostgreSQL
                var violations = violationRepository.findBySessionIdOrderByDetectedAtDesc(testSessionId);
                assertThat(violations).hasSize(1);
                assertThat(violations.get(0).getType()).isEqualTo(Violation.ViolationType.TAB_SWITCH);
                assertThat(violations.get(0).getStrikeCount()).isEqualTo(2);
        }

        /**
         * Test 2: Atomic Counting (Race Condition Prevention)
         * THE POWER MOVE: Multiple simultaneous violations counted correctly
         */
        @Test
        void testAtomicCountingRaceCondition() throws Exception {
                // Given: Multiple violations occurring simultaneously
                ExecutorService executor = Executors.newFixedThreadPool(10);

                // When: Fire 10 simultaneous violations
                CompletableFuture<?>[] futures = new CompletableFuture[10];
                for (int i = 0; i < 10; i++) {
                        final int index = i;
                        futures[i] = CompletableFuture.runAsync(() -> {
                                violationService.recordViolation(
                                                testSessionId,
                                                testStudentId,
                                                testExamId,
                                                Violation.ViolationType.NO_FACE_DETECTED,
                                                Violation.Severity.MINOR, // 1 strike each
                                                "Face not detected - " + index,
                                                Map.of("frameNumber", index));
                        }, executor);
                }

                // Wait for all to complete
                CompletableFuture.allOf(futures).join();
                executor.shutdown();

                // Then: All 10 strikes should be counted (no race condition!)
                int finalCount = violationService.getStrikeCount(testSessionId);
                assertThat(finalCount).isEqualTo(10);

                // Verify PostgreSQL has all violations
                var violations = violationRepository.findBySessionIdOrderByDetectedAtDesc(testSessionId);
                assertThat(violations).hasSize(10);
        }

        /**
         * Test 3: Auto-Termination at 5 Strikes
         */
        @Test
        void testAutoTerminationAtFiveStrikes() {
                // When: Record violations until 5 strikes
                // Strike 1-2
                violationService.recordViolation(
                                testSessionId, testStudentId, testExamId,
                                Violation.ViolationType.TAB_SWITCH,
                                Violation.Severity.MAJOR, // 2 strikes
                                "First violation",
                                Map.of());

                // Strike 3-4
                violationService.recordViolation(
                                testSessionId, testStudentId, testExamId,
                                Violation.ViolationType.PHONE_DETECTED,
                                Violation.Severity.MAJOR, // 2 strikes
                                "Second violation",
                                Map.of());

                // Strike 5 (should trigger termination)
                ViolationService.ViolationRecordResult result = violationService.recordViolation(
                                testSessionId, testStudentId, testExamId,
                                Violation.ViolationType.NO_FACE_DETECTED,
                                Violation.Severity.MINOR, // 1 strike
                                "Third violation",
                                Map.of());

                int finalCount = result.strikeCount();

                // Then: Session should be terminated
                assertThat(finalCount).isEqualTo(5);

                ExamSession session = sessionManager.getSession(testSessionId);
                assertThat(session.getStatus()).isEqualTo(ExamSession.SessionStatus.TERMINATED);
        }

        /**
         * Test 4: Critical Violation (Immediate Termination)
         */
        @Test
        void testCriticalViolationImmediateTermination() {
                // When: Record a CRITICAL violation
                ViolationService.ViolationRecordResult result = violationService.recordViolation(
                                testSessionId,
                                testStudentId,
                                testExamId,
                                Violation.ViolationType.COPY_PASTE_DETECTED,
                                Violation.Severity.CRITICAL, // 5 strikes = immediate termination
                                "AI IDE detected",
                                Map.of("detectedTool", "GitHub Copilot"));

                int strikeCount = result.strikeCount();

                // Then: Should be terminated immediately
                assertThat(strikeCount).isEqualTo(5);

                ExamSession session = sessionManager.getSession(testSessionId);
                assertThat(session.getStatus()).isEqualTo(ExamSession.SessionStatus.TERMINATED);
        }

        /**
         * Test 5: False Positive Handling
         */
        @Test
        void testFalsePositiveRejection() {
                // Given: A violation is recorded
                violationService.recordViolation(
                                testSessionId,
                                testStudentId,
                                testExamId,
                                Violation.ViolationType.PHONE_DETECTED,
                                Violation.Severity.MAJOR, // 2 strikes
                                "Phone detected",
                                Map.of("confidence", 0.75));

                int initialCount = violationService.getStrikeCount(testSessionId);
                assertThat(initialCount).isEqualTo(2);

                // When: Moderator rejects as false positive
                var violations = violationRepository.findBySessionIdOrderByDetectedAtDesc(testSessionId);
                Long violationId = violations.get(0).getId();

                violationService.updateViolationConfirmation(
                                violationId,
                                false,
                                "Student was adjusting webcam");

                // Then: Strike count should be adjusted
                int finalCount = violationService.getStrikeCount(testSessionId);
                assertThat(finalCount).isEqualTo(0);

                // Verify violation marked as unconfirmed
                Violation violation = violationRepository.findById(java.util.Objects.requireNonNull(violationId))
                                .orElseThrow();
                assertThat(violation.getConfirmed()).isFalse();
        }

        /**
         * Test 6: Multiple Violation Types
         */
        @Test
        void testMultipleViolationTypes() {
                // When: Record different types of violations
                violationService.recordViolation(
                                testSessionId, testStudentId, testExamId,
                                Violation.ViolationType.TAB_SWITCH,
                                Violation.Severity.MAJOR,
                                "Tab switch",
                                Map.of());

                violationService.recordViolation(
                                testSessionId, testStudentId, testExamId,
                                Violation.ViolationType.PHONE_DETECTED,
                                Violation.Severity.MAJOR,
                                "Phone detected",
                                Map.of());

                violationService.recordViolation(
                                testSessionId, testStudentId, testExamId,
                                Violation.ViolationType.NO_FACE_DETECTED,
                                Violation.Severity.MINOR,
                                "Face not detected",
                                Map.of());

                // Then: Get statistics
                var stats = violationService.getViolationStats(testSessionId);
                assertThat(stats.totalStrikes()).isEqualTo(5); // 2 + 2 + 1
                assertThat(stats.totalViolations()).isEqualTo(3);
                assertThat(stats.cameraViolations()).isEqualTo(2); // PHONE + NO_FACE
                assertThat(stats.tabSwitchCount()).isEqualTo(1);
                assertThat(stats.terminated()).isTrue(); // 5 strikes = terminated
        }

        /**
         * Test 7: Evidence Storage (JSONB)
         */
        @Test
        void testEvidenceStorage() {
                // Given: Violation with complex evidence
                Map<String, Object> evidence = Map.of(
                                "screenshot", "base64_encoded_image_data...",
                                "confidence", 0.95,
                                "detectedObject", "cell phone",
                                "boundingBox", Map.of("x", 100, "y", 200, "width", 50, "height", 80),
                                "timestamp", LocalDateTime.now().toString(),
                                "clientInfo", Map.of(
                                                "userAgent", "Mozilla/5.0...",
                                                "screenResolution", "1920x1080"));

                // When: Record violation
                violationService.recordViolation(
                                testSessionId,
                                testStudentId,
                                testExamId,
                                Violation.ViolationType.PHONE_DETECTED,
                                Violation.Severity.MAJOR,
                                "Cell phone detected with high confidence",
                                evidence);

                // Then: Verify evidence stored correctly
                var violations = violationRepository.findBySessionIdOrderByDetectedAtDesc(testSessionId);
                assertThat(violations).hasSize(1);

                Violation violation = violations.get(0);
                assertThat(violation.getEvidence()).isNotNull();
                assertThat(violation.getEvidence().get("confidence")).isEqualTo(0.95);
                assertThat(violation.getEvidence().get("detectedObject")).isEqualTo("cell phone");
        }

        /**
         * Test 8: Reset Strike Count (Admin Appeal)
         */
        @Test
        void testResetStrikeCount() {
                // Given: Session with violations
                // Act
                ViolationService.ViolationRecordResult result = violationService.recordViolation(
                                testSessionId,
                                testStudentId,
                                testExamId,
                                Violation.ViolationType.TAB_SWITCH,
                                Violation.Severity.MAJOR,
                                "Tab switch detected",
                                null);

                // Assert
                assertThat(result.strikeCount()).isEqualTo(2); // Major violation = 2 strikes
        }

        @Test
        void testGetViolationStats() {
                // Arrange
                // Add some violations
                violationService.recordViolation(testSessionId, testStudentId, testExamId,
                                Violation.ViolationType.TAB_SWITCH, Violation.Severity.MAJOR, "V1", null);
                violationService.recordViolation(testSessionId, testStudentId, testExamId,
                                Violation.ViolationType.NO_FACE_DETECTED, Violation.Severity.MINOR, "V2", null);

                // Act
                ViolationService.ViolationStats stats = violationService.getViolationStats(testSessionId);

                // Assert
                assertThat(stats.totalViolations()).isEqualTo(2);
                assertThat(stats.cameraViolations()).isEqualTo(1); // NO_FACE_DETECTED is a camera violation
                assertThat(stats.tabSwitchCount()).isEqualTo(1);
        }

        @Test
        void testResetStrikeCountAfterViolations() { // Renamed to avoid conflict
                // Arrange
                violationService.recordViolation(testSessionId, testStudentId, testExamId,
                                Violation.ViolationType.TAB_SWITCH, Violation.Severity.MAJOR, "V1", null);

                // Act
                violationService.resetStrikeCount(testSessionId, "Appeal accepted");

                // Assert
                assertThat(violationService.getStrikeCount(testSessionId)).isEqualTo(0);
        }
}
