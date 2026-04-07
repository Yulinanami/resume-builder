package com.resumebuilder.springaibackend.dto;

import java.util.List;

public record RagQueryResponse(String answer, List<RagSource> sources) {
}
