package com.examportal.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final AmazonS3 s3Client;

    @Value("${minio.bucket-name:test-cases}")
    private String bucketName;

    /**
     * Upload test cases JSON to MinIO/S3
     */
    public void uploadTestCases(String questionId, String testCasesJson) {
        String key = "question_" + questionId + ".json";
        byte[] content = testCasesJson.getBytes(StandardCharsets.UTF_8);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        metadata.setContentType("application/json");

        try {
            s3Client.putObject(bucketName, key, new ByteArrayInputStream(content), metadata);
            log.info("Uploaded test cases for question {} to S3 bucket {}", questionId, bucketName);
        } catch (Exception e) {
            log.error("Failed to upload to S3", e);
            throw new RuntimeException("Storage Error: Failed to save test cases");
        }
    }

    /**
     * Fetch test cases from S3
     * Caches result to avoid repeated S3 calls
     */
    @Cacheable(value = "testCases", key = "#questionId")
    public String getTestCases(String questionId) {
        String key = "question_" + questionId + ".json";
        try {
            S3Object s3Object = s3Client.getObject(bucketName, key);
            return IOUtils.toString(s3Object.getObjectContent());
        } catch (IOException e) {
            log.error("Failed to read from S3", e);
            throw new RuntimeException("Storage Error: Failed to read test cases");
        } catch (Exception e) {
            log.error("Test cases not found in S3 for question {}", questionId, e);
            throw new RuntimeException("Test cases not found");
        }
    }
}
