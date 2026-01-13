package com.examportal.service;

import com.examportal.dto.StudentResultDTO;
import com.examportal.entity.*;
import com.examportal.repository.StudentAttemptRepository;
import com.examportal.repository.TestRepository;
import com.examportal.repository.UserRepository;
import com.examportal.security.DepartmentSecurityService;

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
        log.info("Entering getDetailedResults for testId: {}", testId);
        try {
            Test test = testRepository.findById(java.util.Objects.requireNonNull(testId))
                    .orElseThrow(() -> new RuntimeException("Test not found"));

            // Verify department access
            departmentSecurityService.verifyDepartmentAccess(test.getDepartment());

            // Get all attempts for this test first (Source of Truth for who took it)
            List<StudentAttempt> attempts = attemptRepository.findByTestId(testId);
            log.info("Found {} attempts for testId: {}", attempts.size(), testId);

            Map<Long, StudentAttempt> attemptMap = attempts.stream()
                    .collect(Collectors.toMap(StudentAttempt::getStudentId, a -> a));

            // Get all students in the department (to track absentees)
            List<User> departmentStudents = userRepository.findByRoleNameAndDepartment("STUDENT", test.getDepartment());
            log.info("Found {} department students for testId: {}", departmentStudents.size(), testId);

            // Combine IDs: Students who attempted + Students in department
            java.util.Set<Long> allStudentIds = new java.util.HashSet<>();
            allStudentIds.addAll(attemptMap.keySet());
            allStudentIds.addAll(departmentStudents.stream().map(User::getId).toList());

            // Fetch all relevant user entities
            List<User> students = userRepository.findAllById(allStudentIds);
            log.info("Processing results for {} total unique students", students.size());

            List<StudentResultDTO> results = new ArrayList<>();

            for (User student : students) {
                try {
                    StudentAttempt attempt = attemptMap.get(student.getId());

                    String fullName = (student.getFirstName() != null ? student.getFirstName() : "") +
                            " " +
                            (student.getLastName() != null ? student.getLastName() : "");
                    fullName = fullName.trim();
                    if (fullName.isEmpty())
                        fullName = student.getUsername();

                    StudentResultDTO dto = StudentResultDTO.builder()
                            .studentId(student.getId())
                            .studentName(fullName)
                            .registrationNumber(student.getUsername()) // Using username as Reg No
                            .build();

                    if (attempt != null) {
                        Double score = attempt.getScore() != null ? attempt.getScore() : 0.0;
                        Double totalMarks = attempt.getTotalMarks() != null ? attempt.getTotalMarks() : 0.0;

                        dto.setScore(score);
                        dto.setTotalMarks(totalMarks);
                        dto.setPercentage(totalMarks > 0 ? (score / totalMarks) * 100 : 0.0);
                        dto.setSubmittedAt(attempt.getSubmittedAt());
                        dto.setViolationCount(attempt.getViolationCount());
                        dto.setAutoSubmitted(attempt.getAutoSubmitted());
                        dto.setAttendanceStatus(StudentResultDTO.AttendanceStatus.ATTENDED);

                        // Get violation summary
                        Map<ViolationType, Long> violationSummary = proctorLogService
                                .getViolationSummary(attempt.getId());
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
                } catch (Exception e) {
                    log.error("Error processing student {} for test {}", student.getId(), testId, e);
                    // Continue processing other students
                }
            }

            log.info("Exiting getDetailedResults with {} results", results.size());
            return results;
        } catch (Exception e) {
            log.error("CRITICAL FAILURE in getDetailedResults for testId: {}", testId, e);
            throw e;
        }
    }

    public List<StudentResultDTO> getAttendanceReport(Long testId) {
        return getDetailedResults(testId);
    }

    public byte[] exportToExcel(Long testId) throws IOException {
        Test test = testRepository.findById(java.util.Objects.requireNonNull(testId))
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
        headerRow.createCell(3).setCellValue("Score");

        // Data
        int rowNum = 1;
        for (StudentResultDTO result : results) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(result.getRegistrationNumber());
            row.createCell(1).setCellValue(result.getStudentName());
            row.createCell(2).setCellValue(result.getAttendanceStatus().toString());

            Double score = result.getScore() != null ? result.getScore() : 0.0;
            // If they didn't attend, maybe leave it blank or 0?
            // result.getScore() defaults to 0.0 in getDetailedResults if not attended, so
            // using that.
            row.createCell(3).setCellValue(score);
        }

        // Auto-size columns
        for (int i = 0; i < 4; i++) {
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
                row.createCell(2).setCellValue(result.getScore() != null ? result.getScore() : 0.0);
                row.createCell(3).setCellValue(result.getTotalMarks() != null ? result.getTotalMarks() : 0.0);
                row.createCell(4).setCellValue(
                        String.format("%.2f%%", result.getPercentage() != null ? result.getPercentage() : 0.0));
                row.createCell(5).setCellValue(result.getViolationCount() != null ? result.getViolationCount() : 0);
                row.createCell(6).setCellValue(Boolean.TRUE.equals(result.getAutoSubmitted()) ? "Yes" : "No");
                row.createCell(7).setCellValue(
                        result.getSubmittedAt() != null
                                ? result.getSubmittedAt().format(formatter)
                                : "");
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
