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
import java.util.Map;

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

        BulkUploadResult result = questionService.bulkUpload(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/paste-upload")
    public ResponseEntity<BulkUploadResult> pasteUpload(@RequestBody Map<String, String> payload) {
        String text = payload.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        BulkUploadResult result = questionService.processTextPaste(text);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<QuestionDTO> cloneQuestion(@PathVariable Long id) {
        QuestionDTO cloned = questionService.cloneQuestion(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }
}
