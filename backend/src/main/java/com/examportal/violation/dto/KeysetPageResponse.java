package com.examportal.violation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 10: Keyset Pagination Response
 * 
 * Includes cursor for next page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeysetPageResponse<T> {
    
    private T content;
    private LocalDateTime nextCursor; // For keyset pagination
    private boolean hasNext;
    private int pageSize;
    private long totalElements; // Optional (requires count query)
    
    public static <T> KeysetPageResponse<T> of(T content, 
                                                LocalDateTime nextCursor, 
                                                boolean hasNext, 
                                                int pageSize) {
        return KeysetPageResponse.<T>builder()
                .content(content)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .pageSize(pageSize)
                .build();
    }
}
