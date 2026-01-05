package com.examportal.service;

import com.examportal.dto.StudentResultDTO;
import com.examportal.entity.*;
import com.examportal.repository.StudentAttemptRepository;
import com.examportal.repository.TestRepository;
import com.examportal.repository.UserRepository;
import com.examportal.security.DepartmentSecurityService;
import com.examportal.service.ProctorLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TestRepository testRepository;
    private final StudentAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final ProctorLogService proctorLogService;
    private final DepartmentSecurityService departmentSecurityService;

    public List<StudentResultDTO> getDetailedResults(Long testId) {
        Test test = testRepository.findById(testId)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        // Verify department access
        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());
        
        // Get all students in the department
        List<User> students = userRepository.findByRoleNameAndDepartment("STUDENT", test.getDepartment());
        
        // Get all attempts for this test
        List<StudentAttempt> attempts = attemptRepository.findByTestId(testId);
        Map<Long, StudentAttempt> attemptMap = attempts.stream()
            .collect(Collectors.toMap(StudentAttempt::getStudentId, a -> a));
        
        List<StudentResultDTO> results = new ArrayList<>();
        
        for (User student : students) {
            StudentAttempt attempt = attemptMap.get(student.getId());
            
            String fullName = (student.getFirstName() != null ? student.getFirstName() : "") + 
                             " " + 
                             (student.getLastName() != null ? student.getLastName() : "");
            fullName = fullName.trim();
            if (fullName.isEmpty()) fullName = student.getUsername();

            StudentResultDTO dto = StudentResultDTO.builder()
                .studentId(student.getId())
                .studentName(fullName)
                .registrationNumber(student.getUsername()) // Using username as Reg No
                .build();
            
            if (attempt != null) {
                dto.setScore(attempt.getScore());
                dto.setTotalMarks(attempt.getTotalMarks());
                dto.setPercentage(attempt.getTotalMarks() > 0 
                    ? (attempt.getScore() / attempt.getTotalMarks()) * 100 
                    : 0.0);
                dto.setSubmittedAt(attempt.getSubmittedAt());
                dto.setViolationCount(attempt.getViolationCount());
                dto.setAutoSubmitted(attempt.getAutoSubmitted());
                dto.setAttendanceStatus(StudentResultDTO.AttendanceStatus.ATTENDED);
                
                // Get violation summary
                Map<ViolationType, Long> violationSummary = 
                    proctorLogService.getViolationSummary(attempt.getId());
                dto.setViolationSummary(violationSummary);
            } else {
                dto.setAttendanceStatus(StudentResultDTO.AttendanceStatus.NOT_ATTENDED);
                dto.setScore(0.0);
                dto.setTotalMarks(0.0);
                dto.setPercentage(0.0);
                dto.setViolationCount(0);
                dto.setAutoSubmitted(false);
            }
            
            results.add(dto);
        }
        
        return results;
    }

    public List<StudentResultDTO> getAttendanceReport(Long testId) {
        return getDetailedResults(testId);
    }

    public byte[] exportToExcel(Long testId) throws IOException {
        Test test = testRepository.findById(testId)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        List<StudentResultDTO> results = getDetailedResults(testId);
        
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Create Attendance Sheet
            Sheet attendanceSheet = workbook.createSheet("Attendance");
            createAttendanceSheet(attendanceSheet, results, test);
            
            // Create Results Sheet
            Sheet resultsSheet = workbook.createSheet("Results");
            createResultsSheet(resultsSheet, results, test);
            
            // Create Violations Sheet
            Sheet violationsSheet = workbook.createSheet("Violations");
            createViolationsSheet(violationsSheet, results, test);
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createAttendanceSheet(Sheet sheet, List<StudentResultDTO> results, Test test) {
        // Header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Registration Number");
        headerRow.createCell(1).setCellValue("Student Name");
        headerRow.createCell(2).setCellValue("Status");
        
        // Data
        int rowNum = 1;
        for (StudentResultDTO result : results) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(result.getRegistrationNumber());
            row.createCell(1).setCellValue(result.getStudentName());
            row.createCell(2).setCellValue(result.getAttendanceStatus().toString());
        }
        
        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createResultsSheet(Sheet sheet, List<StudentResultDTO> results, Test test) {
        // Header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Reg No");
        headerRow.createCell(1).setCellValue("Name");
        headerRow.createCell(2).setCellValue("Score");
        headerRow.createCell(3).setCellValue("Total");
        headerRow.createCell(4).setCellValue("Percentage");
        headerRow.createCell(5).setCellValue("Violations");
        headerRow.createCell(6).setCellValue("Auto-Submitted");
        headerRow.createCell(7).setCellValue("Submitted At");
        
        // Data
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        for (StudentResultDTO result : results) {
            if (result.getAttendanceStatus() == StudentResultDTO.AttendanceStatus.ATTENDED) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(result.getRegistrationNumber());
                row.createCell(1).setCellValue(result.getStudentName());
                row.createCell(2).setCellValue(result.getScore());
                row.createCell(3).setCellValue(result.getTotalMarks());
                row.createCell(4).setCellValue(String.format("%.2f%%", result.getPercentage()));
                row.createCell(5).setCellValue(result.getViolationCount());
                row.createCell(6).setCellValue(result.getAutoSubmitted() ? "Yes" : "No");
                row.createCell(7).setCellValue(
                    result.getSubmittedAt() != null 
                        ? result.getSubmittedAt().format(formatter) 
                        : ""
                );
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createViolationsSheet(Sheet sheet, List<StudentResultDTO> results, Test test) {
        // Header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Reg No");
        headerRow.createCell(1).setCellValue("Name");
        headerRow.createCell(2).setCellValue("Total Violations");
        headerRow.createCell(3).setCellValue("Violation Details");
        
        // Data
        int rowNum = 1;
        for (StudentResultDTO result : results) {
            if (result.getViolationCount() != null && result.getViolationCount() > 0) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(result.getRegistrationNumber());
                row.createCell(1).setCellValue(result.getStudentName());
                row.createCell(2).setCellValue(result.getViolationCount());
                
                // Format violation summary
                if (result.getViolationSummary() != null) {
                    String details = result.getViolationSummary().entrySet().stream()
                        .map(e -> e.getKey() + ": " + e.getValue())
                        .collect(Collectors.joining(", "));
                    row.createCell(3).setCellValue(details);
                }
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
