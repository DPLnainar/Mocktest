package com.examportal.execution.client;

import feign.Logger;
import feign.Request;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Judge0 Client Configuration
 * 
 * Configures Feign client for Judge0 API communication
 */
@Configuration
@Slf4j
public class Judge0ClientConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options requestOptions() {
        // Connection timeout: 10s, Read timeout: 30s (for slow executions)
        return new Request.Options(10, TimeUnit.SECONDS, 30, TimeUnit.SECONDS, true);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Judge0 API error: {} - {}", response.status(), response.reason());
            
            switch (response.status()) {
                case 429:
                    return new Judge0RateLimitException("Judge0 rate limit exceeded");
                case 503:
                    return new Judge0ServiceUnavailableException("Judge0 service unavailable");
                default:
                    return new Judge0Exception("Judge0 API error: " + response.status());
            }
        };
    }

    public static class Judge0Exception extends RuntimeException {
        public Judge0Exception(String message) {
            super(message);
        }
    }

    public static class Judge0RateLimitException extends Judge0Exception {
        public Judge0RateLimitException(String message) {
            super(message);
        }
    }

    public static class Judge0ServiceUnavailableException extends Judge0Exception {
        public Judge0ServiceUnavailableException(String message) {
            super(message);
        }
    }
}
