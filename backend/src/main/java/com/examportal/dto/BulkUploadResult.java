package com.examportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResult {

    private int successCount;
    private int errorCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<Long> questionIds = new ArrayList<>();

    public void addError(int rowNumber, String message) {
        errors.add("Row " + rowNumber + ": " + message);
    }
}
