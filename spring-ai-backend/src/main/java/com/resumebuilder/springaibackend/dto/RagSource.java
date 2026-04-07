package com.resumebuilder.springaibackend.dto;

import java.util.Map;

public record RagSource(String sourceId, String content, Map<String, Object> metadata) {
}
