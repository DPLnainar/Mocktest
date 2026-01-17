package com.examportal.service;

import com.examportal.dto.BulkUploadResult;
import com.examportal.dto.CodingQuestionDTO;
import com.examportal.dto.MCQQuestionDTO;
import com.examportal.dto.QuestionDTO;
import com.examportal.dto.TestDTO;
import com.examportal.dto.TestQuestionDTO;
import com.examportal.entity.*;
import com.examportal.repository.QuestionRepository;
import com.examportal.repository.TestRepository;
import com.examportal.security.DepartmentSecurityService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final DepartmentSecurityService departmentSecurityService;

    @PersistenceContext
    private EntityManager entityManager;

    public TestService(TestRepository testRepository,
            QuestionRepository questionRepository,
            DepartmentSecurityService departmentSecurityService) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.departmentSecurityService = departmentSecurityService;
    }

    @Transactional
    public TestDTO createTest(TestDTO dto) {
        departmentSecurityService.verifyDepartmentAccess(dto.getDepartment());

        Test test = new Test();
        test.setTitle(dto.getTitle());
        test.setDescription(dto.getDescription());
        test.setDepartment(dto.getDepartment());
        test.setStartDateTime(dto.getStartDateTime());
        test.setEndDateTime(dto.getEndDateTime());
        test.setDurationMinutes(dto.getDurationMinutes());
        test.setType(TestType.valueOf(dto.getType()));
        test.setInstructions(dto.getInstructions());
        test.setStatus(TestStatus.PUBLISHED);
        test.setTestType(dto.getTestType());

        test = testRepository.save(test);

        if (dto.getTestQuestions() != null) {
            for (int i = 0; i < dto.getTestQuestions().size(); i++) {
                var tqDto = dto.getTestQuestions().get(i);
                Question question = questionRepository.findById(Objects.requireNonNull(tqDto.getQuestionId()))
                        .orElseThrow(() -> new RuntimeException("Question not found: " + tqDto.getQuestionId()));

                test.addQuestion(
                        question,
                        tqDto.getMarks() != null ? tqDto.getMarks() : question.getMarks(),
                        tqDto.getSectionName() != null ? tqDto.getSectionName() : "General",
                        tqDto.getOrderIndex() != null ? tqDto.getOrderIndex() : (i + 1));
            }
            test = testRepository.save(test);
        }

        return mapToDTO(test);
    }

    public List<TestDTO> getTestsForModerator() {
        if (departmentSecurityService.isCurrentUserAdmin()) {
            return testRepository.findAll().stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } else {
            String department = departmentSecurityService.getCurrentUserDepartment();
            return testRepository.findByDepartment(department).stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        }
    }

    public TestDTO getTestById(Long id) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test not found"));
        return mapToDTO(test);
    }

    @Transactional
    public TestDTO updateTest(Long id, TestDTO dto) {
        Test test = testRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Test not found"));

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
            log.info("DETACH STRATEGY: Updating questions for test {}. New count: {}", id,
                    dto.getTestQuestions().size());

            // 1. Clear the collection
            test.getTestQuestions().clear();
            testRepository.saveAndFlush(test);

            // 2. DETACH the entity completely
            entityManager.detach(test);

            // 3. Native DELETE and flush
            entityManager.createNativeQuery("DELETE FROM test_questions WHERE test_id = :tid")
                    .setParameter("tid", id)
                    .executeUpdate();
            entityManager.flush();

            // 4. Clear entire persistence context
            entityManager.clear();

            // 5. Reload test with fresh context
            test = testRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test not found after reload"));

            // 6. Add new questions
            java.util.Set<Long> seenIds = new java.util.HashSet<>();
            for (int i = 0; i < dto.getTestQuestions().size(); i++) {
                var tqDto = dto.getTestQuestions().get(i);
                Long qId = tqDto.getQuestionId();
                if (qId == null || seenIds.contains(qId))
                    continue;
                seenIds.add(qId);

                Question question = questionRepository.findById(qId)
                        .orElseThrow(() -> new RuntimeException("Question not found with ID: " + qId));

                log.info("Adding link: Test {} <-> Question {}", id, qId);
                test.addQuestion(
                        question,
                        tqDto.getMarks() != null ? tqDto.getMarks() : question.getMarks(),
                        tqDto.getSectionName() != null ? tqDto.getSectionName() : "General",
                        tqDto.getOrderIndex() != null ? tqDto.getOrderIndex() : seenIds.size());
            }
        }

        try {
            Test saved = testRepository.save(test);
            log.info("Successfully saved test {}. Final question count: {}", id, saved.getTestQuestions().size());
            return mapToDTO(saved);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to finalize test update for {}. Error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public TestDTO updateTestStatus(Long id, String status) {
        Test test = testRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Test not found"));
        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());

        test.setStatus(TestStatus.valueOf(status));
        return mapToDTO(testRepository.save(test));
    }

    @Transactional
    public void deleteTest(Long id) {
        log.info("Attempting to delete test with ID: {}", id);
        Test test = testRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Test not found with ID: " + id));

        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());

        try {
            testRepository.delete(test);
            log.info("Successfully deleted test with ID: {}", id);
        } catch (Exception e) {
            log.error("Error deleting test with ID: {}. Error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete test: " + e.getMessage(), e);
        }
    }

    private TestDTO mapToDTO(Test test) {
        return TestDTO.builder()
                .id(test.getId())
                .title(test.getTitle())
                .description(test.getDescription())
                .department(test.getDepartment())
                .startDateTime(test.getStartDateTime())
                .endDateTime(test.getEndDateTime())
                .durationMinutes(test.getDurationMinutes())
                .type(test.getType().name())
                .status(test.getStatus().name())
                .testType(test.getTestType())
                .instructions(test.getInstructions())
                .testQuestions(test.getTestQuestions().stream()
                        .map(this::mapQuestionToDTO)
                        .collect(Collectors.toList()))
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .build();
    }

    private TestQuestionDTO mapQuestionToDTO(TestQuestion tq) {
        Question q = tq.getQuestion();

        // Create the appropriate DTO based on question type
        QuestionDTO questionDTO;

        if (q.getType() == QuestionType.MCQ) {
            MCQQuestionDTO mcqDTO = new MCQQuestionDTO();
            mcqDTO.setId(q.getId());
            mcqDTO.setType(q.getType());
            mcqDTO.setQuestionText(q.getQuestionText());
            mcqDTO.setMarks(q.getMarks());
            mcqDTO.setDepartment(q.getDepartment());
            mcqDTO.setExplanation(q.getExplanation());
            mcqDTO.setOptionA(q.getOptionA());
            mcqDTO.setOptionB(q.getOptionB());
            mcqDTO.setOptionC(q.getOptionC());
            mcqDTO.setOptionD(q.getOptionD());
            mcqDTO.setCorrectOption(q.getCorrectOption());
            questionDTO = mcqDTO;
        } else {
            // Coding question
            CodingQuestionDTO codingDTO = new CodingQuestionDTO();
            codingDTO.setId(q.getId());
            codingDTO.setType(q.getType());
            codingDTO.setQuestionText(q.getQuestionText());
            codingDTO.setMarks(q.getMarks());
            codingDTO.setDepartment(q.getDepartment());
            codingDTO.setExplanation(q.getExplanation());
            codingDTO.setStarterCode(q.getStarterCode());
            codingDTO.setTestCases(q.getTestCases());
            codingDTO.setAllowedLanguageIds(q.getAllowedLanguageIds());
            questionDTO = codingDTO;
        }

        return TestQuestionDTO.builder()
                .questionId(q.getId())
                .question(questionDTO)
                .marks(tq.getMarks())
                .sectionName(tq.getSectionName())
                .orderIndex(tq.getOrderIndex())
                .build();
    }

    public List<TestDTO> getAvailableTestsForStudent() {
        List<Test> tests = testRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        return tests.stream()
                .filter(test -> test.getStatus() == TestStatus.PUBLISHED &&
                        test.getStartDateTime().isBefore(now) &&
                        test.getEndDateTime().isAfter(now))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BulkUploadResult uploadQuestionsToTest(Long testId, MultipartFile file) {
        return new BulkUploadResult();
    }
}
