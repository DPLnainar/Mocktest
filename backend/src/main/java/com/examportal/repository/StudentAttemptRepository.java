package com.examportal.repository;

import com.examportal.entity.AttemptStatus;
import com.examportal.entity.StudentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAttemptRepository extends JpaRepository<StudentAttempt, Long> {
    
    Optional<StudentAttempt> findByTestIdAndStudentId(Long testId, Long studentId);
    
    List<StudentAttempt> findByTestId(Long testId);
    
    List<StudentAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);
    
    List<StudentAttempt> findByTestIdAndStatus(Long testId, AttemptStatus status);
    
    boolean existsByTestIdAndStudentId(Long testId, Long studentId);
}
