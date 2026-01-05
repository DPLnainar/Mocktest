package com.examportal.repository;

import com.examportal.entity.ScreenRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenRecordingRepository extends JpaRepository<ScreenRecording, Long> {
    List<ScreenRecording> findByAttemptIdOrderByChunkNumberAsc(Long attemptId);
    void deleteByAttemptId(Long attemptId);
}
