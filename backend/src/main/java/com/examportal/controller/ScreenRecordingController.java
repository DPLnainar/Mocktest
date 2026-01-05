package com.examportal.controller;

import com.examportal.entity.ScreenRecording;
import com.examportal.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/proctor/recording")
@RequiredArgsConstructor
public class ScreenRecordingController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<Map<String, Object>> uploadRecordingChunk(
            @RequestParam("file") MultipartFile file,
            @RequestParam("attemptId") Long attemptId,
            @RequestParam("chunkNumber") Integer chunkNumber) {
        
        try {
            log.info("Receiving recording chunk {} for attempt {}", chunkNumber, attemptId);
            
            ScreenRecording recording = fileStorageService.saveRecordingChunk(attemptId, chunkNumber, file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("chunkNumber", chunkNumber);
            response.put("recordingId", recording.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to upload recording chunk", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{attemptId}")
    @PreAuthorize("hasAnyAuthority('MODERATOR', 'ADMIN')")
    public ResponseEntity<List<ScreenRecording>> getRecordings(@PathVariable Long attemptId) {
        List<ScreenRecording> recordings = fileStorageService.getRecordingsForAttempt(attemptId);
        return ResponseEntity.ok(recordings);
    }

    @DeleteMapping("/{attemptId}")
    @PreAuthorize("hasAnyAuthority('MODERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteRecordings(@PathVariable Long attemptId) {
        fileStorageService.deleteRecordingsForAttempt(attemptId);
        return ResponseEntity.ok().build();
    }
}
