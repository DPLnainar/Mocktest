package com.examportal.service;

import com.examportal.dto.TestDTO;
import com.examportal.entity.Question;
import com.examportal.entity.Test;
import com.examportal.repository.QuestionRepository;
import com.examportal.repository.TestRepository;
import com.examportal.security.DepartmentSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final DepartmentSecurityService departmentSecurityService;
    private final QuestionService questionService;

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
            .createdBy(userId)
            .questions(new HashSet<>())
            .build();
        
        // Add questions if provided
        if (dto.getQuestionIds() != null && !dto.getQuestionIds().isEmpty()) {
            List<Question> questions = questionRepository.findByIdIn(dto.getQuestionIds());
            test.getQuestions().addAll(questions);
        }
        
        test = testRepository.save(test);
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
        
        // Find tests that haven't ended yet
        List<Test> tests = testRepository.findByDepartmentAndEndDateTimeAfterOrderByStartDateTimeAsc(
            department, now
        );
        
        return tests.stream().map(this::mapToDTO).toList();
    }

    public TestDTO getTestById(Long id) {
        Test test = testRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        // Verify department access
        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());
        
        return mapToDTO(test);
    }

    public boolean isTestActive(Long testId) {
        Test test = testRepository.findById(testId)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(test.getStartDateTime()) && !now.isAfter(test.getEndDateTime());
    }

    public boolean isTestUpcoming(Long testId) {
        Test test = testRepository.findById(testId)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        return LocalDateTime.now().isBefore(test.getStartDateTime());
    }

    public boolean isTestExpired(Long testId) {
        Test test = testRepository.findById(testId)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        return LocalDateTime.now().isAfter(test.getEndDateTime());
    }

    @Transactional
    public TestDTO updateTest(Long id, TestDTO dto) {
        Test test = testRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        // Verify department access
        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());
        
        test.setTitle(dto.getTitle());
        test.setDescription(dto.getDescription());
        test.setStartDateTime(dto.getStartDateTime());
        test.setEndDateTime(dto.getEndDateTime());
        test.setDurationMinutes(dto.getDurationMinutes());
        
        if (dto.getQuestionIds() != null) {
            List<Question> questions = questionRepository.findByIdIn(dto.getQuestionIds());
            test.setQuestions(new HashSet<>(questions));
        }
        
        test = testRepository.save(test);
        return mapToDTO(test);
    }

    @Transactional
    public void deleteTest(Long id) {
        Test test = testRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Test not found"));
        
        // Verify department access
        departmentSecurityService.verifyDepartmentAccess(test.getDepartment());
        
        testRepository.delete(test);
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
        dto.setCreatedAt(test.getCreatedAt());
        dto.setUpdatedAt(test.getUpdatedAt());
        
        if (test.getQuestions() != null) {
            dto.setQuestionIds(test.getQuestions().stream()
                .map(Question::getId)
                .toList());
            
            // Map full question objects for frontend
            dto.setQuestions(test.getQuestions().stream()
                .map(questionService::mapQuestionToDTO)
                .toList());
        }
        
        return dto;
    }
}
