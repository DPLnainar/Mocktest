package com.examportal.controller;

import com.examportal.service.StudentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/history")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('STUDENT')")
public class StudentHistoryController {

    private final StudentHistoryService historyService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getHistory() {
        List<Map<String, Object>> history = historyService.getStudentHistory();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/tests/{testId}/review")
    public ResponseEntity<Map<String, Object>> getTestReview(@PathVariable Long testId) {
        Map<String, Object> review = historyService.getTestReview(testId);
        return ResponseEntity.ok(review);
    }
}
