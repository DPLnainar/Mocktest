package com.examportal.violation.service;

import com.examportal.monitoring.model.StudentStatus;
import com.examportal.violation.entity.Violation;
import com.examportal.violation.repository.ViolationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ViolationService {

    @Autowired
    private ViolationRepository violationRepository;

    public Violation logViolation(Violation violation) {
        violation.setTimestamp(LocalDateTime.now());
        return violationRepository.save(violation);
    }

    public List<Violation> getStudentViolations(Long studentId) {
        return violationRepository.findByStudentId(studentId);
    }

    public void processViolation(Violation v, StudentStatus status) {
        // Update counts based on violation type
        int currentViolations = status.getViolationCount() != null ? status.getViolationCount() : 0;
        status.setViolationCount(currentViolations + 1);

        // Update status color based on simple logic
        if (status.getViolationCount() > 5) {
            status.setStatusColor(StudentStatus.StatusColor.RED);
        } else if (status.getViolationCount() > 2) {
            status.setStatusColor(StudentStatus.StatusColor.YELLOW);
        } else {
            status.setStatusColor(StudentStatus.StatusColor.GREEN);
        }
    }
}
