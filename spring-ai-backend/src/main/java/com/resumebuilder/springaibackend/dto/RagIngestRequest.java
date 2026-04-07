package com.resumebuilder.springaibackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RagIngestRequest(
        @NotEmpty(message = "documents cannot be empty") List<@Valid RagDocumentInput> documents
) {
}
