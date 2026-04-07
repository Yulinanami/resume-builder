package com.resumebuilder.springaibackend.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "message cannot be empty") String message,
        Boolean sanitizeOutput
) {
}