package com.examportal.controller;

import com.examportal.dto.BulkUploadResult;
import com.examportal.dto.QuestionDTO;
import com.examportal.entity.QuestionType;
import com.examportal.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MODERATOR')")
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionDTO> createQuestion(@Valid @RequestBody QuestionDTO dto) {
        QuestionDTO created = questionService.createQuestion(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<QuestionDTO>> getAllQuestions(
            @RequestParam(required = false) QuestionType type) {
        List<QuestionDTO> questions;
        if (type != null) {
            questions = questionService.getQuestionsByType(type);
        } else {
            questions = questionService.getQuestions();
        }
        return ResponseEntity.ok(questions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getQuestionById(@PathVariable Long id) {
        QuestionDTO question = questionService.getQuestionById(id);
        return ResponseEntity.ok(question);
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<BulkUploadResult> bulkUpload(
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            throw new RuntimeException("Only .xlsx files are supported");
        }
        
        BulkUploadResult result = questionService.bulkUploadFromExcel(file);
        return ResponseEntity.ok(result);
    }
}
