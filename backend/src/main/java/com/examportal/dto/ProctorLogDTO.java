package com.examportal.dto;

import com.examportal.entity.ViolationType;
import lombok.Data;
import java.util.Map;

@Data
public class ProctorLogDTO {
    private ViolationType eventType;
    private Map<String, Object> metadata;
}
