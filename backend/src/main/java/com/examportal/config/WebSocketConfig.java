package com.examportal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration with RabbitMQ Broker Relay
 * 
 * Architecture:
 * - External RabbitMQ broker for horizontal scaling
 * - Topic: /topic/exam/{examId}/monitoring (broadcast to all moderators)
 * - User Queue: /user/queue/violations (private messages to students)
 * - Heartbeat: 10s intervals for connection health
 * 
 * Scalability: With RabbitMQ relay, multiple server instances can share
 * WebSocket sessions - Server A can broadcast to users on Server B
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    @Value("${spring.rabbitmq.virtual-host}")
    private String rabbitVirtualHost;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable RabbitMQ broker relay for scalability
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(rabbitHost)
                .setRelayPort(rabbitPort)
                .setClientLogin(rabbitUsername)
                .setClientPasscode(rabbitPassword)
                .setSystemLogin(rabbitUsername)
                .setSystemPasscode(rabbitPassword)
                .setVirtualHost(rabbitVirtualHost)
                .setHeartbeatValue(new long[]{10000, 10000}); // 10s heartbeat

        // Application destination prefix for client-to-server messages
        registry.setApplicationDestinationPrefixes("/app");

        // User-specific destination prefix
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure proper CORS in production
                .withSockJS()
                .setHeartbeatTime(25000); // Client heartbeat
    }

    /**
     * Task scheduler for heartbeat mechanism
     */
    private ThreadPoolTaskScheduler createTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
