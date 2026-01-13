package com.examportal.service;

import com.examportal.entity.ScreenRecording;
import com.examportal.repository.ScreenRecordingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final ScreenRecordingRepository recordingRepository;
    private static final String UPLOAD_DIR = "uploads/recordings";

    public ScreenRecording saveRecordingChunk(Long attemptId, Integer chunkNumber, MultipartFile file)
            throws IOException {
        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR, "attempt-" + attemptId);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file
        String fileName = "chunk-" + chunkNumber + ".webm";
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Saved recording chunk {} for attempt {}, size: {} bytes",
                chunkNumber, attemptId, file.getSize());

        // Save metadata to database
        ScreenRecording recording = ScreenRecording.builder()
                .attemptId(attemptId)
                .chunkNumber(chunkNumber)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .build();

        return recordingRepository.save(java.util.Objects.requireNonNull(recording));
    }

    public List<ScreenRecording> getRecordingsForAttempt(Long attemptId) {
        return recordingRepository.findByAttemptIdOrderByChunkNumberAsc(attemptId);
    }

    public void deleteRecordingsForAttempt(Long attemptId) {
        try {
            // Delete files
            Path uploadPath = Paths.get(UPLOAD_DIR, "attempt-" + attemptId);
            if (Files.exists(uploadPath)) {
                Files.walk(uploadPath)
                        .sorted((a, b) -> -a.compareTo(b)) // Reverse order to delete files before directories
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error("Failed to delete file: {}", path, e);
                            }
                        });
            }

            // Delete database records
            recordingRepository.deleteByAttemptId(attemptId);
            log.info("Deleted all recordings for attempt {}", attemptId);
        } catch (IOException e) {
            log.error("Failed to delete recordings for attempt {}", attemptId, e);
        }
    }
}
