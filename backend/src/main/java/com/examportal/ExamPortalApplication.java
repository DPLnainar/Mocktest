package com.examportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot Application
 * Enterprise-Grade Examination & Placement Portal
 * 
 * Key Features:
 * - ANTLR-based logic verification
 * - Real-time WebSocket monitoring with RabbitMQ
 * - Redis atomic violation counters
 * - Judge0 sandboxed code execution
 * - Department-level RBAC
 */
@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class ExamPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamPortalApplication.class, args);
        System.out.println("""
            
            ╔══════════════════════════════════════════════════════════════╗
            ║  Exam Portal Backend Started Successfully                    ║
            ║  ---------------------------------------------------------- ║
            ║  Features Enabled:                                           ║
            ║  ✓ Spring Security + JWT                                     ║
            ║  ✓ PostgreSQL with HikariCP (Pool: 20)                       ║
            ║  ✓ Redis Atomic Counters                                     ║
            ║  ✓ RabbitMQ WebSocket Broker                                 ║
            ║  ✓ Judge0 Code Execution                                     ║
            ║  ✓ Resilience4j Circuit Breaker                              ║
            ║  ---------------------------------------------------------- ║
            ║  Monitoring: http://localhost:8080/actuator/prometheus       ║
            ╚══════════════════════════════════════════════════════════════╝
            """);
    }
}
