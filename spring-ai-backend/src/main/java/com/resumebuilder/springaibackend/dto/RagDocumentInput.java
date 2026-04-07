package com.resumebuilder.springaibackend.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record RagDocumentInput(
        String sourceId,
        @NotBlank(message = "document content cannot be empty") String content,
        Map<String, Object> metadata
) {
}
