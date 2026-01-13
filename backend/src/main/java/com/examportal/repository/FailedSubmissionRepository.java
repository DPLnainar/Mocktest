package com.examportal.repository;

import com.examportal.entity.FailedSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FailedSubmissionRepository extends JpaRepository<FailedSubmission, Long> {

    List<FailedSubmission> findByStudentId(Long studentId);

    List<FailedSubmission> findByAttemptId(Long attemptId);

    @Query("SELECT COUNT(f) FROM FailedSubmission f WHERE f.failedAt >= :since")
    Long countFailuresSince(LocalDateTime since);

    @Query("SELECT f FROM FailedSubmission f WHERE f.failedAt >= :since ORDER BY f.failedAt DESC")
    List<FailedSubmission> findRecentFailures(LocalDateTime since);
}
