package com.examportal.service;

import com.examportal.entity.*;
import com.examportal.repository.ProctorLogRepository;
import com.examportal.repository.StudentAttemptRepository;
import com.examportal.security.DepartmentSecurityService;
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
    private final DepartmentSecurityService departmentSecurityService;

    private static final int AUTO_SUBMIT_THRESHOLD = 5;

    @Transactional
    public ProctorLog logViolation(Long attemptId, ViolationType type, Map<String, Object> metadata) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
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

        savedLog = proctorLogRepository.save(savedLog);

        // Increment violation count
        attempt.setViolationCount(attempt.getViolationCount() + 1);
        attemptRepository.save(attempt);

        log.info("Violation logged: {} for attempt {} (total: {})", 
            type, attemptId, attempt.getViolationCount());

        return savedLog;
    }

    public long getViolationCount(Long attemptId) {
        return proctorLogRepository.countByAttemptId(attemptId);
    }

    public boolean checkAutoSubmitThreshold(Long attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));
        
        return attempt.getViolationCount() >= AUTO_SUBMIT_THRESHOLD;
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
            case MULTIPLE_FACES, PHONE_DETECTED, CAMERA_DETECTED -> ViolationSeverity.HIGH;
            case TAB_SWITCH, FULLSCREEN_EXIT, NO_FACE -> ViolationSeverity.MEDIUM;
            case COPY_ATTEMPT, PASTE_ATTEMPT, DEVTOOLS_OPENED -> ViolationSeverity.LOW;
        };
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
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
