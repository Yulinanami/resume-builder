package com.resumebuilder.springaibackend.dto;

import java.time.LocalDateTime;

public record InterviewSessionSummaryResponse(
        String sessionId,
        String mode,
        String status,
        int durationMinutes,
        int elapsedSeconds,
        int messageCount,
        Integer totalScore,
        Boolean passed,
        String lastMessagePreview,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
