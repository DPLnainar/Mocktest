package com.examportal.service;

import com.examportal.dto.BulkUploadResult;
import com.examportal.dto.CodingQuestionDTO;
import com.examportal.dto.MCQQuestionDTO;
import com.examportal.dto.QuestionDTO;
import com.examportal.entity.Question;
import com.examportal.entity.QuestionType;
import com.examportal.repository.QuestionRepository;
import com.examportal.security.DepartmentSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Question question = questionRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Question not found"));

        departmentSecurityService.verifyDepartmentAccess(question.getDepartment());
        return mapToDTO(question);
    }

    @Transactional
    public QuestionDTO cloneQuestion(Long id) {
        Question original = questionRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Question not found"));

        departmentSecurityService.verifyDepartmentAccess(original.getDepartment());

        // Create a deep copy
        Question cloned = Question.builder()
                .questionText(original.getQuestionText() + " (Copy)")
                .type(original.getType())
                .marks(original.getMarks())
                .department(original.getDepartment())
                .explanation(original.getExplanation())
                .build();

        // Copy MCQ-specific fields
        if (original.getType() == QuestionType.MCQ) {
            cloned.setOptionA(original.getOptionA());
            cloned.setOptionB(original.getOptionB());
            cloned.setOptionC(original.getOptionC());
            cloned.setOptionD(original.getOptionD());
            cloned.setCorrectOption(original.getCorrectOption());
        }

        // Copy Coding-specific fields
        if (original.getType() == QuestionType.CODING) {
            cloned.setAllowedLanguageIds(original.getAllowedLanguageIds());
            cloned.setStarterCode(original.getStarterCode());
            cloned.setTestCases(original.getTestCases());
            cloned.setConstraints(original.getConstraints());
        }

        cloned = questionRepository.save(java.util.Objects.requireNonNull(cloned));
        return mapToDTO(cloned);
    }

    @Transactional
    public BulkUploadResult bulkUpload(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null)
            throw new RuntimeException("Invalid file");

        if (filename.endsWith(".xlsx"))
            return bulkUploadFromExcel(file);
        if (filename.endsWith(".csv"))
            return bulkUploadFromCSV(file);
        if (filename.endsWith(".docx"))
            return bulkUploadFromWord(file);
        if (filename.endsWith(".pdf"))
            return bulkUploadFromPDF(file);

        throw new RuntimeException("Unsupported file format: " + filename);
    }

    @Transactional
    public BulkUploadResult bulkUploadFromCSV(MultipartFile file) {
        String department = departmentSecurityService.getCurrentUserDepartment();
        BulkUploadResult result = BulkUploadResult.builder().build();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            // Skip header
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                try {
                    Question question = parseCsvRowToQuestion(row, department);
                    validateQuestion(question);
                    question = questionRepository.save(java.util.Objects.requireNonNull(question));
                    result.getQuestionIds().add(question.getId());
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception e) {
                    result.setErrorCount(result.getErrorCount() + 1);
                    result.addError(i + 1, e.getMessage());
                }
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Failed to read CSV: " + e.getMessage());
        }
        return result;
    }

    @Transactional
    public BulkUploadResult bulkUploadFromWord(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder fullText = new StringBuilder();
            for (XWPFParagraph para : document.getParagraphs()) {
                fullText.append(para.getText()).append("\n");
            }
            return processTextPaste(fullText.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Word file: " + e.getMessage());
        }
    }

    @Transactional
    public BulkUploadResult bulkUploadFromPDF(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return processTextPaste(text);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF: " + e.getMessage());
        }
    }

    @Transactional
    public BulkUploadResult processTextPaste(String text) {
        String department = departmentSecurityService.getCurrentUserDepartment();
        BulkUploadResult result = BulkUploadResult.builder().build();

        // Split by "Q:" or "Question:" to identify individual questions
        String[] chunks = text.split("(?i)(?=Q:|Question:|\\d+\\.)");
        int index = 1;

        for (String chunk : chunks) {
            if (chunk.trim().isEmpty())
                continue;
            try {
                Question question = parseTextToQuestion(chunk, department);
                if (question != null) {
                    validateQuestion(question);
                    question = questionRepository.save(java.util.Objects.requireNonNull(question));
                    result.getQuestionIds().add(question.getId());
                    result.setSuccessCount(result.getSuccessCount() + 1);
                }
            } catch (Exception e) {
                result.setErrorCount(result.getErrorCount() + 1);
                result.addError(index, e.getMessage());
            }
            index++;
        }
        return result;
    }

    private Question parseCsvRowToQuestion(String[] row, String department) {
        if (row.length < 3)
            throw new IllegalArgumentException("Insufficient data in row");

        QuestionType type = QuestionType.valueOf(row[0].toUpperCase());
        Question question = new Question();
        question.setType(type);
        question.setQuestionText(row[1]);
        question.setMarks(Integer.parseInt(row[2]));
        question.setDepartment(department);

        if (type == QuestionType.MCQ && row.length >= 8) {
            question.setOptionA(row[3]);
            question.setOptionB(row[4]);
            question.setOptionC(row[5]);
            question.setOptionD(row[6]);
            question.setCorrectOption(row[7]);
        } else if (type == QuestionType.CODING && row.length >= 10) {
            question.setAllowedLanguageIds(List.of(Integer.parseInt(row[8])));
            question.setStarterCode(row[9]);
        }

        if (row.length > 10)
            question.setExplanation(row[10]);

        return question;
    }

    private Question parseTextToQuestion(String chunk, String department) {
        // Simple heuristic parsing for text/PDF/Word
        // Looking for Question Text, Options A-D, and Correct Answer
        Question question = new Question();
        question.setDepartment(department);
        question.setMarks(5); // Default marks

        Pattern textPattern = Pattern.compile("(?i)(?:Q:|Question:|\\d+\\.)\\s*(.*)");
        Pattern optionAPattern = Pattern.compile("(?i)A[\\).:]\\s*(.*)");
        Pattern optionBPattern = Pattern.compile("(?i)B[\\).:]\\s*(.*)");
        Pattern optionCPattern = Pattern.compile("(?i)C[\\).:]\\s*(.*)");
        Pattern optionDPattern = Pattern.compile("(?i)D[\\).:]\\s*(.*)");
        Pattern correctPattern = Pattern.compile("(?i)(?:Correct|Answer):\\s*([A-D])");

        Matcher textMatcher = textPattern.matcher(chunk);
        if (textMatcher.find()) {
            question.setQuestionText(textMatcher.group(1).trim());
        } else if (!chunk.trim().isEmpty()) {
            question.setQuestionText(chunk.trim());
        } else {
            return null;
        }

        Matcher aM = optionAPattern.matcher(chunk);
        Matcher bM = optionBPattern.matcher(chunk);
        Matcher cM = optionCPattern.matcher(chunk);
        Matcher dM = optionDPattern.matcher(chunk);
        Matcher corM = correctPattern.matcher(chunk);

        if (aM.find() && bM.find() && cM.find() && dM.find()) {
            question.setType(QuestionType.MCQ);
            question.setOptionA(aM.group(1).trim());
            question.setOptionB(bM.group(1).trim());
            question.setOptionC(cM.group(1).trim());
            question.setOptionD(dM.group(1).trim());

            if (corM.find()) {
                question.setCorrectOption(corM.group(1).toUpperCase());
            } else {
                throw new IllegalArgumentException("MCQ found but no correct answer specified (Expected 'Correct: A')");
            }
        } else {
            // Default to Coding if no clear MCQ options found, but user might just want a
            // simple text-based MCQ
            // Better to assume MCQ if it looks like one, or just fail if it's too ambiguous
            // For now, let's treat it as MCQ with default options if it's not clearly
            // coding
            if (chunk.contains("Language ID:")) {
                question.setType(QuestionType.CODING);
                Pattern langPattern = Pattern.compile("(?i)Language ID:\\s*(\\d+)");
                Matcher langM = langPattern.matcher(chunk);
                if (langM.find())
                    question.setAllowedLanguageIds(List.of(Integer.parseInt(langM.group(1))));
            } else {
                // If marks are missing, use default. Otherwise try to find marks: 10
                Pattern marksPattern = Pattern.compile("(?i)Marks:\\s*(\\d+)");
                Matcher marksM = marksPattern.matcher(chunk);
                if (marksM.find())
                    question.setMarks(Integer.parseInt(marksM.group(1)));

                question.setType(QuestionType.MCQ);
                // If it's a simple text block without A/B/C/D, it's not a valid MCQ yet
                if (question.getOptionA() == null)
                    throw new IllegalArgumentException("Question text found but MCQ options are missing");
            }
        }

        return question;
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
                if (row == null)
                    continue;

                try {
                    Question question = parseRowToQuestion(row, department);
                    validateQuestion(question);
                    question = questionRepository.save(java.util.Objects.requireNonNull(question));
                    result.getQuestionIds().add(question.getId());
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
        // Expected columns: Type, QuestionText, Marks, OptionA, OptionB, OptionC,
        // OptionD, CorrectOption, LanguageId, StarterCode, Explanation

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
            Integer langId = getCellIntValue(row.getCell(8));
            if (langId != null) {
                question.setAllowedLanguageIds(List.of(langId));
            }
            question.setStarterCode(getCellValue(row.getCell(9)));
        }

        question.setExplanation(getCellValue(row.getCell(10)));

        return question;
    }

    private String getCellValue(Cell cell) {
        if (cell == null)
            return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Integer getCellIntValue(Cell cell) {
        if (cell == null)
            return null;

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
            if (question.getAllowedLanguageIds() == null || question.getAllowedLanguageIds().isEmpty()) {
                throw new IllegalArgumentException("Coding questions must have at least one allowed language");
            }
        }
    }

    private Question mapToEntity(QuestionDTO dto) {
        Question question = new Question();
        question.setId(dto.getId());
        question.setType(dto.getType());
        question.setQuestionText(dto.getQuestionText());
        question.setMarks(dto.getMarks());
        question.setExplanation(dto.getExplanation());
        // Department is set in createQuestion

        if (dto instanceof com.examportal.dto.MCQQuestionDTO) {
            com.examportal.dto.MCQQuestionDTO mcq = (com.examportal.dto.MCQQuestionDTO) dto;
            question.setOptionA(mcq.getOptionA());
            question.setOptionB(mcq.getOptionB());
            question.setOptionC(mcq.getOptionC());
            question.setOptionD(mcq.getOptionD());
            question.setCorrectOption(mcq.getCorrectOption());
        } else if (dto instanceof CodingQuestionDTO) {
            CodingQuestionDTO coding = (CodingQuestionDTO) dto;
            question.setAllowedLanguageIds(coding.getAllowedLanguageIds());
            question.setTestCases(coding.getTestCases());
            question.setConstraints(coding.getConstraints());
            question.setStarterCode(coding.getStarterCode());
        }

        return question;
    }

    private QuestionDTO mapToDTO(Question question) {
        QuestionDTO dto;
        if (question.getType() == QuestionType.MCQ) {
            MCQQuestionDTO mcq = new MCQQuestionDTO();
            mcq.setOptionA(question.getOptionA());
            mcq.setOptionB(question.getOptionB());
            mcq.setOptionC(question.getOptionC());
            mcq.setOptionD(question.getOptionD());
            mcq.setCorrectOption(question.getCorrectOption());
            dto = mcq;
        } else if (question.getType() == QuestionType.CODING) {
            CodingQuestionDTO coding = new CodingQuestionDTO();
            coding.setAllowedLanguageIds(question.getAllowedLanguageIds());
            coding.setTestCases(question.getTestCases());
            coding.setConstraints(question.getConstraints());
            coding.setStarterCode(question.getStarterCode());
            dto = coding;
        } else {
            // Should not happen, but fallback to anonymous subclass or throw
            throw new IllegalArgumentException("Unknown question type: " + question.getType());
        }

        dto.setId(question.getId());
        dto.setType(question.getType());
        dto.setQuestionText(question.getQuestionText());
        dto.setMarks(question.getMarks());
        dto.setDepartment(question.getDepartment());
        dto.setExplanation(question.getExplanation());
        return dto;
    }

    // Public method for TestService to use
    public QuestionDTO mapQuestionToDTO(Question question) {
        return mapToDTO(question);
    }
}
