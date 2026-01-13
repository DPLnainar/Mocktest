package com.examportal.service;

import com.examportal.dto.TestCaseUploadResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Service for uploading test case files to MinIO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinIOUploadService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.test-cases:test-cases}")
    private String testCasesBucket;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Upload test case file to MinIO
     */
    public TestCaseUploadResponse uploadTestCase(MultipartFile file, String questionId) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("File size exceeds 10MB limit");
            }

            // Generate unique key
            String timestamp = String.valueOf(System.currentTimeMillis());

            String fileName = file.getOriginalFilename();
            String s3Key = String.format("test-cases/%s_%s_%s",
                    questionId != null ? questionId : "temp",
                    timestamp,
                    fileName);

            // Upload to MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(testCasesBucket)
                            .object(s3Key)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            log.info("Uploaded test case file to MinIO: {}", s3Key);

            // Build response
            return TestCaseUploadResponse.builder()
                    .s3Key(s3Key)
                    .s3Url("s3://" + testCasesBucket + "/" + s3Key)
                    .fileName(fileName)
                    .fileSize(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload test case file to MinIO", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    /**
     * Delete test case file from MinIO
     */
    public void deleteTestCase(String s3Key) {
        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(testCasesBucket)
                            .object(s3Key)
                            .build());
            log.info("Deleted test case file from MinIO: {}", s3Key);
        } catch (Exception e) {
            log.error("Failed to delete test case file from MinIO: {}", s3Key, e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }
}
