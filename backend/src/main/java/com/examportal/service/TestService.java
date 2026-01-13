package com.examportal.service;

import com.examportal.dto.BulkUploadResult;
import com.examportal.dto.TestDTO;
import com.examportal.dto.TestQuestionDTO;
import com.examportal.entity.Question;
import com.examportal.entity.Test;
import com.examportal.entity.TestStatus;
import com.examportal.entity.TestType;
import com.examportal.repository.QuestionRepository;
import com.examportal.repository.TestRepository;
import com.examportal.security.DepartmentSecurityService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service

@Slf4j
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final DepartmentSecurityService departmentSecurityService;
    private final QuestionService questionService;

    public TestService(TestRepository testRepository,
            QuestionRepository questionRepository,
            DepartmentSecurityService departmentSecurityService,
            QuestionService questionService) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.departmentSecurityService = departmentSecurityService;
        this.questionService = questionService;
    }

    /**
     * Upload a file (CSV/Excel/Word/PDF) containing questions and attach them to
     * the test.
     * Returns the BulkUploadResult from QuestionService and also links the created
     * questions
     * to the specified test with default marks and section.
     */
    @Transactional
    public BulkUploadResult uploadQuestionsToTest(Long testId, MultipartFile file) {
        // 1️⃣ Parse and persist questions via QuestionService
        BulkUploadResult uploadResult = questionService.bulkUpload(file);

        // 2️⃣ Load the test (ensuring department access)
        Test test = testRepository.findById(java.util.Objects.requireNonNull(testId))
                .orElseThrow(() -> new RuntimeException("Test not found: " + testId));
        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());

        // 3️⃣ Attach each newly created question to the test
        int order = test.getTestQuestions() != null ? test.getTestQuestions().size() : 0;
        for (Long qId : uploadResult.getQuestionIds()) {
            Question question = questionRepository.findById(java.util.Objects.requireNonNull(qId))
                    .orElseThrow(() -> new RuntimeException("Question not found after upload: " + qId));
            // Use default marks = question.marks (or 1) and generic section "Imported"
            test.addQuestion(question,
                    question.getMarks() != null ? question.getMarks() : 1,
                    "Imported",
                    order++);
        }
        testRepository.save(test);
        return uploadResult;
    }

    @Transactional
    public TestDTO createTest(TestDTO dto) {
        String department = departmentSecurityService.getCurrentUserDepartment();
        Long userId = departmentSecurityService.getCurrentUserId();

        // Validate time constraints
        if (dto.getEndDateTime().isBefore(dto.getStartDateTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Test test = Test.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .department(department)
                .startDateTime(dto.getStartDateTime())
                .endDateTime(dto.getEndDateTime())
                .durationMinutes(dto.getDurationMinutes())
                .type(TestType.valueOf(dto.getType()))
                .status(dto.getStatus() != null ? TestStatus.valueOf(dto.getStatus()) : TestStatus.DRAFT)
                .testType(dto.getTestType())
                .instructions(dto.getInstructions())
                .createdBy(userId)
                .build();

        // Add questions if provided
        if (dto.getTestQuestions() != null && !dto.getTestQuestions().isEmpty()) {
            for (int i = 0; i < dto.getTestQuestions().size(); i++) {
                var tqDto = dto.getTestQuestions().get(i);
                Question question = questionRepository.findById(java.util.Objects.requireNonNull(tqDto.getQuestionId()))
                        .orElseThrow(() -> new RuntimeException("Question not found: " + tqDto.getQuestionId()));

                test.addQuestion(
                        question,
                        tqDto.getMarks() != null ? tqDto.getMarks() : question.getMarks(),
                        tqDto.getSectionName() != null ? tqDto.getSectionName() : "General",
                        tqDto.getOrderIndex() != null ? tqDto.getOrderIndex() : i);
            }
        }

        test = testRepository.save(java.util.Objects.requireNonNull(test));
        return mapToDTO(test);
    }

    public List<TestDTO> getTestsForModerator() {
        String department = departmentSecurityService.getCurrentUserDepartment();
        List<Test> tests = testRepository.findByDepartment(department);
        return tests.stream().map(this::mapToDTO).toList();
    }

    public List<TestDTO> getAvailableTestsForStudent() {
        String department = departmentSecurityService.getCurrentUserDepartment();
        LocalDateTime now = LocalDateTime.now();

        // Find tests that haven't ended yet for User's Dept, MODERATOR, or General
        List<String> departments = Arrays.asList(department, "MODERATOR", "General");

        List<Test> tests = testRepository.findByDepartmentInAndEndDateTimeAfterOrderByStartDateTimeAsc(
                departments, now);

        // Filter strictly for PUBLISHED tests
        return tests.stream()
                .filter(test -> test.getStatus() == TestStatus.PUBLISHED)
                .map(this::mapToDTO)
                .toList();
    }

    public TestDTO getTestById(Long id) {
        Test test = testRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Test not found"));
        // Verify department access
        String department = test.getDepartment();
        if (!department.equals("MODERATOR") && !department.equals("General")) {
            departmentSecurityService.verifyDepartmentAccess(department);
        }
        return mapToDTO(test);
    }

    public boolean isTestActive(Long testId) {
        Test test = testRepository.findById(java.util.Objects.requireNonNull(testId))
                .orElseThrow(() -> new RuntimeException("Test not found"));
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(test.getStartDateTime()) && !now.isAfter(test.getEndDateTime());
    }

    public boolean isTestUpcoming(Long testId) {
        Test test = testRepository.findById(java.util.Objects.requireNonNull(testId))
                .orElseThrow(() -> new RuntimeException("Test not found"));
        return LocalDateTime.now().isBefore(test.getStartDateTime());
    }

    public boolean isTestExpired(Long testId) {
        Test test = testRepository.findById(java.util.Objects.requireNonNull(testId))
                .orElseThrow(() -> new RuntimeException("Test not found"));
        return LocalDateTime.now().isAfter(test.getEndDateTime());
    }

    @Transactional
    public TestDTO updateTest(Long id, TestDTO dto) {
        Test test = testRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Test not found"));
        // Verify department access
        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());

        test.setTitle(dto.getTitle());
        test.setDescription(dto.getDescription());
        test.setStartDateTime(dto.getStartDateTime());
        test.setEndDateTime(dto.getEndDateTime());
        test.setDurationMinutes(dto.getDurationMinutes());
        test.setType(TestType.valueOf(dto.getType()));

        if (dto.getStatus() != null) {
            test.setStatus(TestStatus.valueOf(dto.getStatus()));
        }
        test.setTestType(dto.getTestType());
        test.setInstructions(dto.getInstructions());

        if (dto.getTestQuestions() != null) {
            test.getTestQuestions().clear();
            for (int i = 0; i < dto.getTestQuestions().size(); i++) {
                var tqDto = dto.getTestQuestions().get(i);
                Question question = questionRepository.findById(java.util.Objects.requireNonNull(tqDto.getQuestionId()))
                        .orElseThrow(() -> new RuntimeException("Question not found: " + tqDto.getQuestionId()));

                test.addQuestion(
                        question,
                        tqDto.getMarks() != null ? tqDto.getMarks() : question.getMarks(),
                        tqDto.getSectionName() != null ? tqDto.getSectionName() : "General",
                        tqDto.getOrderIndex() != null ? tqDto.getOrderIndex() : i);
            }
        }

        test = testRepository.save(test);
        return mapToDTO(testRepository.save(test));
    }

    @Transactional
    public TestDTO updateTestStatus(Long id, String status) {
        Test test = testRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Test not found"));
        // Verify department access
        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());

        test.setStatus(TestStatus.valueOf(status));
        return mapToDTO(testRepository.save(test));
    }

    @Transactional
    public void deleteTest(Long id) {
        log.info("Attempting to delete test with ID: {}", id);
        Test test = testRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Test not found with ID: " + id));

        // Verify department access
        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());

        try {
            // Delete the test (TestQuestions will be cascade deleted)
            testRepository.delete(test);
            log.info("Successfully deleted test with ID: {}", id);
        } catch (Exception e) {
            log.error("Error deleting test with ID: {}. Error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete test: " + e.getMessage(), e);
        }
    }

    private TestDTO mapToDTO(Test test) {
        TestDTO dto = new TestDTO();
        dto.setId(test.getId());
        dto.setTitle(test.getTitle());
        dto.setDescription(test.getDescription());
        dto.setDepartment(test.getDepartment());
        dto.setStartDateTime(test.getStartDateTime());
        dto.setEndDateTime(test.getEndDateTime());
        dto.setDurationMinutes(test.getDurationMinutes());
        dto.setType(test.getType().name());
        dto.setStatus(test.getStatus().name());
        dto.setTestType(test.getTestType());
        dto.setInstructions(test.getInstructions());
        dto.setCreatedAt(test.getCreatedAt());
        dto.setUpdatedAt(test.getUpdatedAt());

        if (test.getTestQuestions() != null) {
            dto.setTestQuestions(test.getTestQuestions().stream()
                    .map(tq -> TestQuestionDTO.builder()
                            .id(tq.getId())
                            .questionId(tq.getQuestion().getId())
                            .question(questionService.mapQuestionToDTO(tq.getQuestion()))
                            .marks(tq.getMarks())
                            .sectionName(tq.getSectionName())
                            .orderIndex(tq.getOrderIndex())
                            .build())
                    .toList());
        }
        return dto;
    }
}
