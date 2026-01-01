package com.examportal.monitoring.listener;

import com.examportal.monitoring.model.StudentStatus;
import com.examportal.monitoring.service.MonitoringBroadcastService;
import com.examportal.monitoring.service.SessionManagerService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

/**
 * WebSocket Event Listener
 * 
 * Listens for WebSocket connection/disconnection events
 * Broadcasts connection status to moderators
 */
@Component
public class WebSocketEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SessionManagerService sessionManager;
    private final MonitoringBroadcastService broadcastService;

    public WebSocketEventListener(SessionManagerService sessionManager, MonitoringBroadcastService broadcastService) {
        this.sessionManager = sessionManager;
        this.broadcastService = broadcastService;
    }

    /**
     * Handle WebSocket connection established
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("New WebSocket connection: {}", sessionId);
        
        // Connection details will be handled in the /connect message handler
        // This is just for logging and monitoring
    }

    /**
     * Handle WebSocket disconnection
     * Broadcast offline status to moderators
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String webSocketSessionId = headerAccessor.getSessionId();
        
        log.info("WebSocket disconnected: {}", webSocketSessionId);

        try {
            // Get exam session by WebSocket session ID
            Long sessionId = sessionManager.getSessionIdByWebSocket(webSocketSessionId);
            
            if (sessionId != null) {
                var examSession = sessionManager.getSession(sessionId);
                
                if (examSession != null) {
                    log.info("Student {} disconnected from exam {}", 
                            examSession.getStudentId(), examSession.getExamId());

                    // Broadcast offline status to moderators
                    StudentStatus status = new StudentStatus();
                    status.setStudentId(examSession.getStudentId());
                    status.setStudentName(examSession.getStudentName());
                    status.setSessionId(sessionId);
                    status.setConnectionStatus(StudentStatus.ConnectionStatus.OFFLINE);
                    status.setLastActivity(LocalDateTime.now());

                    broadcastService.broadcastStudentStatus(examSession.getExamId(), status);
                    
                    // Broadcast connection status update
                    broadcastService.broadcastConnectionStatus(
                            examSession.getExamId(), 
                            examSession.getStudentId(), 
                            StudentStatus.ConnectionStatus.OFFLINE
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket disconnection", e);
        }
    }
}
