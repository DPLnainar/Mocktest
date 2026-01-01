package com.examportal.execution.client;

import com.examportal.execution.model.Judge0SubmissionRequest;
import com.examportal.execution.model.Judge0SubmissionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Judge0 Feign Client
 * 
 * Communicates with Judge0 API for code execution
 * Circuit breaker applied via Resilience4j configuration
 */
@FeignClient(
    name = "judge0-client",
    url = "${judge0.api-url}",
    configuration = Judge0ClientConfig.class
)
public interface Judge0Client {

    /**
     * Submit code for execution
     * 
     * @param apiKey Judge0 API key
     * @param request Submission request
     * @return Submission response with token
     */
    @PostMapping("/submissions")
    Judge0SubmissionResponse createSubmission(
        @RequestHeader("X-RapidAPI-Key") String apiKey,
        @RequestHeader("X-RapidAPI-Host") String apiHost,
        @RequestBody Judge0SubmissionRequest request
    );

    /**
     * Get submission result by token
     * 
     * @param apiKey Judge0 API key
     * @param apiHost Judge0 API host
     * @param token Submission token
     * @param fields Fields to include in response
     * @return Submission response with results
     */
    @GetMapping("/submissions/{token}")
    Judge0SubmissionResponse getSubmission(
        @RequestHeader("X-RapidAPI-Key") String apiKey,
        @RequestHeader("X-RapidAPI-Host") String apiHost,
        @PathVariable("token") String token,
        @RequestParam(value = "fields", required = false) String fields
    );

    /**
     * Batch submission creation
     * 
     * @param apiKey Judge0 API key
     * @param apiHost Judge0 API host
     * @param requests Array of submission requests
     * @return Array of submission responses
     */
    @PostMapping("/submissions/batch")
    Judge0SubmissionResponse[] createBatchSubmissions(
        @RequestHeader("X-RapidAPI-Key") String apiKey,
        @RequestHeader("X-RapidAPI-Host") String apiHost,
        @RequestBody Judge0SubmissionRequest[] requests
    );

    /**
     * Get batch submission results
     * 
     * @param apiKey Judge0 API key
     * @param apiHost Judge0 API host
     * @param tokens Comma-separated submission tokens
     * @return Array of submission responses
     */
    @GetMapping("/submissions/batch")
    Judge0SubmissionResponse[] getBatchSubmissions(
        @RequestHeader("X-RapidAPI-Key") String apiKey,
        @RequestHeader("X-RapidAPI-Host") String apiHost,
        @RequestParam("tokens") String tokens
    );
}
