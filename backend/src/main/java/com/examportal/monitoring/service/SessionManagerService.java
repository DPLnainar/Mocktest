package com.examportal.monitoring.service;

import com.examportal.monitoring.model.ExamSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Session Manager Service
 * 
 * Manages WebSocket sessions and exam sessions in Redis
 * Enables reconnection without losing state
 * 
 * Architecture:
 * - Redis stores session data with 4-hour TTL
 * - WebSocket session ID maps to exam session
 * - Heartbeat mechanism detects disconnections
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagerService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SESSION_PREFIX = "exam:session:";
    private static final String WEBSOCKET_PREFIX = "websocket:session:";
    private static final String ACTIVE_SESSIONS_KEY = "exam:sessions:active";
    private static final long SESSION_TTL_HOURS = 4;

    /**
     * Create or update exam session
     */
    public void createSession(ExamSession session) {
        String sessionKey = SESSION_PREFIX + session.getId();
        
        // Store session data
        redisTemplate.opsForHash().put(sessionKey, "studentId", session.getStudentId());
        redisTemplate.opsForHash().put(sessionKey, "examId", session.getExamId());
        redisTemplate.opsForHash().put(sessionKey, "examTitle", session.getExamTitle());
        redisTemplate.opsForHash().put(sessionKey, "studentName", session.getStudentName());
        redisTemplate.opsForHash().put(sessionKey, "department", session.getDepartment());
        redisTemplate.opsForHash().put(sessionKey, "status", session.getStatus().name());
        redisTemplate.opsForHash().put(sessionKey, "violationCount", session.getViolationCount());
        redisTemplate.opsForHash().put(sessionKey, "startedAt", session.getStartedAt().toString());
        redisTemplate.opsForHash().put(sessionKey, "expiresAt", session.getExpiresAt().toString());
        redisTemplate.opsForHash().put(sessionKey, "lastHeartbeat", LocalDateTime.now().toString());
        
        if (session.getWebSocketSessionId() != null) {
            redisTemplate.opsForHash().put(sessionKey, "webSocketSessionId", session.getWebSocketSessionId());
            
            // Map WebSocket session to exam session
            String wsKey = WEBSOCKET_PREFIX + session.getWebSocketSessionId();
            redisTemplate.opsForValue().set(wsKey, session.getId(), SESSION_TTL_HOURS, TimeUnit.HOURS);
        }

        // Add to active sessions set
        redisTemplate.opsForSet().add(ACTIVE_SESSIONS_KEY, session.getId());

        // Set TTL
        redisTemplate.expire(sessionKey, SESSION_TTL_HOURS, TimeUnit.HOURS);

        log.info("Created exam session {} for student {}", session.getId(), session.getStudentId());
    }

    /**
     * Get exam session by ID
     */
    public ExamSession getSession(Long sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        if (!redisTemplate.hasKey(sessionKey)) {
            return null;
        }

        try {
            return ExamSession.builder()
                .id(sessionId)
                .studentId((Long) redisTemplate.opsForHash().get(sessionKey, "studentId"))
                .examId((Long) redisTemplate.opsForHash().get(sessionKey, "examId"))
                .examTitle((String) redisTemplate.opsForHash().get(sessionKey, "examTitle"))
                .studentName((String) redisTemplate.opsForHash().get(sessionKey, "studentName"))
                .department((String) redisTemplate.opsForHash().get(sessionKey, "department"))
                .status(ExamSession.SessionStatus.valueOf(
                    (String) redisTemplate.opsForHash().get(sessionKey, "status")))
                .violationCount((Integer) redisTemplate.opsForHash().get(sessionKey, "violationCount"))
                .startedAt(LocalDateTime.parse((String) redisTemplate.opsForHash().get(sessionKey, "startedAt")))
                .expiresAt(LocalDateTime.parse((String) redisTemplate.opsForHash().get(sessionKey, "expiresAt")))
                .webSocketSessionId((String) redisTemplate.opsForHash().get(sessionKey, "webSocketSessionId"))
                .lastHeartbeat(LocalDateTime.parse((String) redisTemplate.opsForHash().get(sessionKey, "lastHeartbeat")))
                .build();
        } catch (Exception e) {
            log.error("Error parsing session data", e);
            return null;
        }
    }

    /**
     * Get session ID by WebSocket session ID
     */
    public Long getSessionIdByWebSocket(String webSocketSessionId) {
        String wsKey = WEBSOCKET_PREFIX + webSocketSessionId;
        Object sessionId = redisTemplate.opsForValue().get(wsKey);
        return sessionId != null ? (Long) sessionId : null;
    }

    /**
     * Update WebSocket session ID (for reconnection)
     */
    public void updateWebSocketSession(Long sessionId, String newWebSocketSessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        // Remove old WebSocket mapping
        String oldWsSessionId = (String) redisTemplate.opsForHash().get(sessionKey, "webSocketSessionId");
        if (oldWsSessionId != null) {
            redisTemplate.delete(WEBSOCKET_PREFIX + oldWsSessionId);
        }

        // Set new WebSocket session
        redisTemplate.opsForHash().put(sessionKey, "webSocketSessionId", newWebSocketSessionId);
        
        String wsKey = WEBSOCKET_PREFIX + newWebSocketSessionId;
        redisTemplate.opsForValue().set(wsKey, sessionId, SESSION_TTL_HOURS, TimeUnit.HOURS);

        log.info("Updated WebSocket session for exam session {}", sessionId);
    }

    /**
     * Update heartbeat timestamp
     */
    public void updateHeartbeat(Long sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.opsForHash().put(sessionKey, "lastHeartbeat", LocalDateTime.now().toString());
        
        // Extend TTL
        redisTemplate.expire(sessionKey, SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * Update violation count
     */
    public void updateViolationCount(Long sessionId, int violations) {
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.opsForHash().put(sessionKey, "violationCount", violations);
    }

    /**
     * Update session status
     */
    public void updateSessionStatus(Long sessionId, ExamSession.SessionStatus status) {
        String sessionKey = SESSION_PREFIX + sessionId;
        redisTemplate.opsForHash().put(sessionKey, "status", status.name());
        
        if (status != ExamSession.SessionStatus.ACTIVE) {
            // Remove from active sessions
            redisTemplate.opsForSet().remove(ACTIVE_SESSIONS_KEY, sessionId);
        }
    }

    /**
     * Get all active sessions for an exam
     */
    public Set<ExamSession> getActiveSessionsForExam(Long examId) {
        Set<Object> sessionIds = redisTemplate.opsForSet().members(ACTIVE_SESSIONS_KEY);
        
        if (sessionIds == null) {
            return Set.of();
        }

        return sessionIds.stream()
            .map(id -> getSession((Long) id))
            .filter(session -> session != null && session.getExamId().equals(examId))
            .filter(session -> session.getStatus() == ExamSession.SessionStatus.ACTIVE)
            .collect(Collectors.toSet());
    }

    /**
     * Get all active sessions for a department
     */
    public Set<ExamSession> getActiveSessionsForDepartment(String department) {
        Set<Object> sessionIds = redisTemplate.opsForSet().members(ACTIVE_SESSIONS_KEY);
        
        if (sessionIds == null) {
            return Set.of();
        }

        return sessionIds.stream()
            .map(id -> getSession((Long) id))
            .filter(session -> session != null && department.equals(session.getDepartment()))
            .filter(session -> session.getStatus() == ExamSession.SessionStatus.ACTIVE)
            .collect(Collectors.toSet());
    }

    /**
     * Terminate session
     */
    public void terminateSession(Long sessionId) {
        updateSessionStatus(sessionId, ExamSession.SessionStatus.TERMINATED);
        log.info("Terminated exam session {}", sessionId);
    }

    /**
     * Delete session
     */
    public void deleteSession(Long sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        // Get WebSocket session ID before deleting
        String wsSessionId = (String) redisTemplate.opsForHash().get(sessionKey, "webSocketSessionId");
        if (wsSessionId != null) {
            redisTemplate.delete(WEBSOCKET_PREFIX + wsSessionId);
        }

        // Remove from active sessions
        redisTemplate.opsForSet().remove(ACTIVE_SESSIONS_KEY, sessionId);
        
        // Delete session data
        redisTemplate.delete(sessionKey);
        
        log.info("Deleted exam session {}", sessionId);
    }

    /**
     * Get total active session count
     */
    public long getActiveSessionCount() {
        Long count = redisTemplate.opsForSet().size(ACTIVE_SESSIONS_KEY);
        return count != null ? count : 0;
    }
}
