package com.examportal.violation.dto;

import java.time.LocalDateTime;

/**
 * Phase 10: Keyset Pagination Response
 * 
 * Includes cursor for next page
 */
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
        KeysetPageResponse<T> response = new KeysetPageResponse<>();
        response.setContent(content);
        response.setNextCursor(nextCursor);
        response.setHasNext(hasNext);
        response.setPageSize(pageSize);
        return response;
    }

    public T getContent() { return content; }
    public void setContent(T content) { this.content = content; }
    public LocalDateTime getNextCursor() { return nextCursor; }
    public void setNextCursor(LocalDateTime nextCursor) { this.nextCursor = nextCursor; }
    public boolean isHasNext() { return hasNext; }
    public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
}
