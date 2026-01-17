package com.examportal.dto;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkQuestionRequest {
    private List<JsonNode> questions;
}
