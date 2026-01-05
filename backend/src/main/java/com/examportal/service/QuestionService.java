package com.examportal.service;

import com.examportal.dto.BulkUploadResult;
import com.examportal.dto.QuestionDTO;
import com.examportal.entity.Question;
import com.examportal.entity.QuestionType;
import com.examportal.repository.QuestionRepository;
import com.examportal.security.DepartmentSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final DepartmentSecurityService departmentSecurityService;

    @Transactional
    public QuestionDTO createQuestion(QuestionDTO dto) {
        String department = departmentSecurityService.getCurrentUserDepartment();
        
        Question question = mapToEntity(dto);
        question.setDepartment(department);
        
        validateQuestion(question);
        
        question = questionRepository.save(question);
        return mapToDTO(question);
    }

    public List<QuestionDTO> getQuestions() {
        String department = departmentSecurityService.getCurrentUserDepartment();
        List<Question> questions = questionRepository.findByDepartment(department);
        return questions.stream().map(this::mapToDTO).toList();
    }

    public List<QuestionDTO> getQuestionsByType(QuestionType type) {
        String department = departmentSecurityService.getCurrentUserDepartment();
        List<Question> questions = questionRepository.findByDepartmentAndType(department, type);
        return questions.stream().map(this::mapToDTO).toList();
    }

    public QuestionDTO getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Question not found"));
        
        departmentSecurityService.verifyDepartmentAccess(question.getDepartment());
        return mapToDTO(question);
    }

    @Transactional
    public BulkUploadResult bulkUploadFromExcel(MultipartFile file) {
        String department = departmentSecurityService.getCurrentUserDepartment();
        BulkUploadResult result = BulkUploadResult.builder().build();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row (row 0)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                
                try {
                    Question question = parseRowToQuestion(row, department);
                    validateQuestion(question);
                    questionRepository.save(question);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception e) {
                    result.setErrorCount(result.getErrorCount() + 1);
                    result.addError(rowIndex + 1, e.getMessage());
                    log.error("Error processing row {}: {}", rowIndex + 1, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Error reading Excel file", e);
            throw new RuntimeException("Failed to read Excel file: " + e.getMessage());
        }
        
        return result;
    }

    private Question parseRowToQuestion(Row row, String department) {
        // Expected columns: Type, QuestionText, Marks, OptionA, OptionB, OptionC, OptionD, CorrectOption, LanguageId, StarterCode, Explanation
        
        Cell typeCell = row.getCell(0);
        if (typeCell == null) {
            throw new IllegalArgumentException("Type is required");
        }
        
        String typeStr = getCellValue(typeCell).toUpperCase();
        QuestionType type;
        try {
            type = QuestionType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid question type: " + typeStr + ". Must be MCQ or CODING");
        }
        
        Question question = new Question();
        question.setType(type);
        question.setQuestionText(getCellValue(row.getCell(1)));
        question.setMarks(getCellIntValue(row.getCell(2)));
        question.setDepartment(department);
        
        if (type == QuestionType.MCQ) {
            question.setOptionA(getCellValue(row.getCell(3)));
            question.setOptionB(getCellValue(row.getCell(4)));
            question.setOptionC(getCellValue(row.getCell(5)));
            question.setOptionD(getCellValue(row.getCell(6)));
            question.setCorrectOption(getCellValue(row.getCell(7)));
        } else if (type == QuestionType.CODING) {
            question.setLanguageId(getCellIntValue(row.getCell(8)));
            question.setStarterCode(getCellValue(row.getCell(9)));
        }
        
        question.setExplanation(getCellValue(row.getCell(10)));
        
        return question;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Integer getCellIntValue(Cell cell) {
        if (cell == null) return null;
        
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void validateQuestion(Question question) {
        if (question.getType() == QuestionType.MCQ) {
            if (question.getOptionA() == null || question.getOptionB() == null ||
                question.getOptionC() == null || question.getOptionD() == null) {
                throw new IllegalArgumentException("MCQ questions must have all 4 options");
            }
            if (question.getCorrectOption() == null || 
                !question.getCorrectOption().matches("[A-D]")) {
                throw new IllegalArgumentException("MCQ must have valid correct option (A, B, C, or D)");
            }
        } else if (question.getType() == QuestionType.CODING) {
            if (question.getLanguageId() == null) {
                throw new IllegalArgumentException("Coding questions must have languageId");
            }
        }
    }

    private Question mapToEntity(QuestionDTO dto) {
        Question question = new Question();
        question.setId(dto.getId());
        question.setType(dto.getType());
        question.setQuestionText(dto.getQuestionText());
        question.setMarks(dto.getMarks());
        question.setOptionA(dto.getOptionA());
        question.setOptionB(dto.getOptionB());
        question.setOptionC(dto.getOptionC());
        question.setOptionD(dto.getOptionD());
        question.setCorrectOption(dto.getCorrectOption());
        question.setLanguageId(dto.getLanguageId());
        question.setTestCases(dto.getTestCases());
        question.setStarterCode(dto.getStarterCode());
        question.setExplanation(dto.getExplanation());
        return question;
    }

    private QuestionDTO mapToDTO(Question question) {
        QuestionDTO dto = new QuestionDTO();
        dto.setId(question.getId());
        dto.setType(question.getType());
        dto.setQuestionText(question.getQuestionText());
        dto.setMarks(question.getMarks());
        dto.setDepartment(question.getDepartment());
        dto.setOptionA(question.getOptionA());
        dto.setOptionB(question.getOptionB());
        dto.setOptionC(question.getOptionC());
        dto.setOptionD(question.getOptionD());
        dto.setCorrectOption(question.getCorrectOption());
        dto.setLanguageId(question.getLanguageId());
        dto.setTestCases(question.getTestCases());
        dto.setStarterCode(question.getStarterCode());
        dto.setExplanation(question.getExplanation());
        return dto;
    }
    
    // Public method for TestService to use
    public QuestionDTO mapQuestionToDTO(Question question) {
        return mapToDTO(question);
    }
}
