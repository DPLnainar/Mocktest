package com.examportal.controller;

import com.examportal.dto.StudentResultDTO;
import com.examportal.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/tests/{testId}/results")
    public ResponseEntity<List<StudentResultDTO>> getDetailedResults(@PathVariable Long testId) {
        List<StudentResultDTO> results = analyticsService.getDetailedResults(testId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/tests/{testId}/attendance")
    public ResponseEntity<List<StudentResultDTO>> getAttendance(@PathVariable Long testId) {
        List<StudentResultDTO> attendance = analyticsService.getAttendanceReport(testId);
        return ResponseEntity.ok(attendance);
    }

    @GetMapping("/tests/{testId}/export/excel")
    public ResponseEntity<byte[]> exportToExcel(@PathVariable Long testId) {
        try {
            byte[] excelData = analyticsService.exportToExcel(testId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = "test-results-" + testId + "-" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + 
                ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
