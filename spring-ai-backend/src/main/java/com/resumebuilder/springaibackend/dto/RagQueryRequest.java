package com.resumebuilder.springaibackend.dto;

import jakarta.validation.constraints.NotBlank;

public record RagQueryRequest(
        @NotBlank(message = "query cannot be empty") String query,
        Integer topK
) {
}
