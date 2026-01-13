package com.examportal.service;

import com.examportal.dto.VerificationRequest;
import com.examportal.dto.VerificationResponse;
import com.examportal.dto.VerificationResult;
import com.examportal.execution.service.Judge0Service;
import com.examportal.execution.model.ExecutionResult;
import com.examportal.service.ModeratorVerificationService.VerificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Worker service that processes verification requests from RabbitMQ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationWorkerService {

    private final ModeratorVerificationService verificationService;
    private final CodeVerificationService codeVerificationService;
    private final Judge0Service judge0Service;

    /**
     * Listen to verification queue and process requests
     */
    @RabbitListener(queues = "verification_queue")
    public void processVerification(VerificationMessage message) {
        String verificationId = message.getVerificationId();

        try {
            log.info("Processing verification request: {}", verificationId);

            // Update status to PROCESSING
            verificationService.updateVerificationStatus(
                    verificationId,
                    VerificationResponse.VerificationStatus.PROCESSING,
                    null,
                    "Executing solution...");

            // Prepare inputs for batch execution
            List<VerificationRequest.TestCaseData> testCases = message.getTestCases();
            String[] inputs = testCases.stream()
                    .map(tc -> tc.getInput())
                    .toArray(String[]::new);

            // Execute batch (Synchronous wait for moderator)
            // Use moderatorId as studentId for tracking
            ExecutionResult[] results = judge0Service.executeBatch(
                    message.getCode(),
                    message.getLanguageId(),
                    inputs,
                    message.getModeratorId());

            // Process Results
            List<VerificationResult.TestCaseResult> testCaseResults = new ArrayList<>();
            boolean allPassed = true;
            int totalExecutionTime = 0;

            for (int i = 0; i < results.length; i++) {
                ExecutionResult result = results[i];
                VerificationRequest.TestCaseData testCase = testCases.get(i);

                String output = result.getOutput() != null ? result.getOutput().trim() : "";
                // If there is an error in execution (stderr), output might be empty or we
                // should capture error
                boolean passed = "ACCEPTED".equals(result.getStatus().name()) &&
                        output.equals(testCase.getExpectedOutput().trim());

                if (!passed)
                    allPassed = false;

                testCaseResults.add(VerificationResult.TestCaseResult.builder()
                        .input(testCase.getInput())
                        .expectedOutput(testCase.getExpectedOutput())
                        .actualOutput(output)
                        .passed(passed)
                        .error(result.getError())
                        .build());
            }

            // Validate logic constraints if enabled
            List<String> constraintViolations = new ArrayList<>();
            if (message.getConstraints() != null) {
                if (Boolean.TRUE.equals(message.getConstraints().getForbidLoops())) {
                    if (codeVerificationService.hasLoops(message.getCode(), message.getLanguageId())) {
                        constraintViolations.add("Code contains forbidden loops");
                        allPassed = false;
                    }
                }

                if (Boolean.TRUE.equals(message.getConstraints().getRequireRecursion())) {
                    if (!codeVerificationService.hasRecursion(message.getCode(), message.getLanguageId())) {
                        constraintViolations.add("Code must use recursion");
                        allPassed = false;
                    }
                }
            }

            // Build result
            VerificationResult result = VerificationResult.builder()
                    .passed(allPassed)
                    .testCaseResults(testCaseResults)
                    .constraintViolations(constraintViolations)
                    .executionTimeMs(totalExecutionTime)
                    .build();

            // Update final status
            verificationService.updateVerificationStatus(
                    verificationId,
                    allPassed ? VerificationResponse.VerificationStatus.SUCCESS
                            : VerificationResponse.VerificationStatus.FAILED,
                    result,
                    allPassed ? "All tests passed" : "Some tests failed");

            log.info("Verification completed: {} - {}", verificationId, allPassed ? "SUCCESS" : "FAILED");

        } catch (Exception e) {
            log.error("Verification processing failed for: {}", verificationId, e);

            // Update status to ERROR
            verificationService.updateVerificationStatus(
                    verificationId,
                    VerificationResponse.VerificationStatus.ERROR,
                    null,
                    "Verification error: " + e.getMessage());
        }
    }
}
