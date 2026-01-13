package com.examportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for test case file upload to MinIO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseUploadResponse {

    private String s3Key;
    private String s3Url;
    private String fileName;
    private Long fileSize;
    private LocalDateTime uploadedAt;
}
