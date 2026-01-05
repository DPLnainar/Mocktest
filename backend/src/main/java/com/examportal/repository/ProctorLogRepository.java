package com.examportal.repository;

import com.examportal.entity.ProctorLog;
import com.examportal.entity.ViolationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProctorLogRepository extends JpaRepository<ProctorLog, Long> {
    
    List<ProctorLog> findByAttemptIdOrderByTimestampAsc(Long attemptId);
    
    List<ProctorLog> findByTestIdOrderByTimestampDesc(Long testId);
    
    @Query("SELECT COUNT(p) FROM ProctorLog p WHERE p.attemptId = :attemptId")
    long countByAttemptId(Long attemptId);
    
    @Query("SELECT p.eventType, COUNT(p) FROM ProctorLog p WHERE p.attemptId = :attemptId GROUP BY p.eventType")
    List<Object[]> countByEventType(Long attemptId);
}
