package com.examportal.service;

import com.examportal.entity.*;
import com.examportal.repository.ProctorLogRepository;
import com.examportal.repository.StudentAttemptRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProctorLogService {

    private final ProctorLogRepository proctorLogRepository;
    private final StudentAttemptRepository attemptRepository;

    private static final int AUTO_SUBMIT_THRESHOLD = 3;

    @Transactional
    public ProctorLog logViolation(Long attemptId, ViolationType type, Map<String, Object> metadata) {
        StudentAttempt attempt = attemptRepository.findById(java.util.Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        // Determine severity
        ViolationSeverity severity = determineSeverity(type);

        // Get request info
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = request != null ? getClientIpAddress(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        ProctorLog savedLog = ProctorLog.builder()
                .attemptId(attemptId)
                .studentId(attempt.getStudentId())
                .testId(attempt.getTestId())
                .eventType(type)
                .severity(severity)
                .metadata(metadata != null ? metadata : new HashMap<>())
                .timestamp(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        savedLog = proctorLogRepository.save(java.util.Objects.requireNonNull(savedLog));

        // Increment violation count ONLY if it's not INFO severity
        if (severity != ViolationSeverity.INFO) {
            attempt.setViolationCount(attempt.getViolationCount() + 1);
            attemptRepository.save(attempt);
            log.info("Violation logged: {} for attempt {} (total: {})",
                    type, attemptId, attempt.getViolationCount());
        } else {
            log.debug("Informational log saved: {} for attempt {}", type, attemptId);
        }

        return savedLog;
    }

    public long getViolationCount(Long attemptId) {
        return proctorLogRepository.countByAttemptId(attemptId);
    }

    public boolean checkAutoSubmitThreshold(Long attemptId) {
        StudentAttempt attempt = attemptRepository.findById(java.util.Objects.requireNonNull(attemptId))
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        boolean reached = attempt.getViolationCount() >= AUTO_SUBMIT_THRESHOLD;

        if (reached && attempt.getStatus() != AttemptStatus.FROZEN) {
            log.warn("Attempt {} frozen due to {} violations", attemptId, attempt.getViolationCount());
            attempt.setStatus(AttemptStatus.FROZEN);
            attempt.setAutoSubmitted(true);
            attempt.setSubmittedAt(LocalDateTime.now());
            attemptRepository.save(attempt);
        }

        return reached;
    }

    public List<ProctorLog> getViolationTimeline(Long attemptId) {
        return proctorLogRepository.findByAttemptIdOrderByTimestampAsc(attemptId);
    }

    public Map<ViolationType, Long> getViolationSummary(Long attemptId) {
        List<Object[]> results = proctorLogRepository.countByEventType(attemptId);
        Map<ViolationType, Long> summary = new HashMap<>();

        for (Object[] result : results) {
            ViolationType type = (ViolationType) result[0];
            Long count = (Long) result[1];
            summary.put(type, count);
        }

        return summary;
    }

    private ViolationSeverity determineSeverity(ViolationType type) {
        return switch (type) {
            case MULTIPLE_FACES, PHONE_DETECTED, CAMERA_DETECTED, IP_MISMATCH, IP_ADDRESS_CHANGED ->
                ViolationSeverity.HIGH;
            case TAB_SWITCH, FULLSCREEN_EXIT, NO_FACE, KEYSTROKE_ANOMALY -> ViolationSeverity.MEDIUM;
            case COPY_ATTEMPT, PASTE_ATTEMPT, DEVTOOLS_OPENED -> ViolationSeverity.LOW;
            case IP_TRACKED -> ViolationSeverity.INFO;
        };
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headersToCheck = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headersToCheck) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Get first IP if multiple
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
