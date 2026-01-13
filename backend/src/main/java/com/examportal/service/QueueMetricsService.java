package com.examportal.service;

import com.examportal.config.RabbitMQConfig;
import com.examportal.dto.QueuePositionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * Service to provide queue metrics and position information
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueueMetricsService {

    private final RabbitAdmin rabbitAdmin;
    private static final int AVG_PROCESSING_TIME_SECONDS = 3;

    /**
     * Get queue position information for a student's submission
     */
    public QueuePositionResponse getQueuePosition(Long attemptId) {
        try {
            Properties queueProps = rabbitAdmin.getQueueProperties(RabbitMQConfig.QUEUE);

            if (queueProps == null) {
                log.warn("Could not retrieve queue properties");
                return new QueuePositionResponse(0, 0, 0);
            }

            int queueDepth = (Integer) queueProps.getOrDefault("QUEUE_MESSAGE_COUNT", 0);
            int estimatedWait = queueDepth * AVG_PROCESSING_TIME_SECONDS;

            log.debug("Queue depth: {}, estimated wait: {}s", queueDepth, estimatedWait);

            return new QueuePositionResponse(queueDepth, estimatedWait, queueDepth);
        } catch (Exception e) {
            log.error("Error getting queue position", e);
            return new QueuePositionResponse(0, 0, 0);
        }
    }
}
