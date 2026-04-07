package com.resumebuilder.springaibackend.dto;

import com.resumebuilder.springaibackend.dto.InterviewTurnRequest.ResumeSnapshot;
import com.resumebuilder.springaibackend.dto.InterviewTurnResponse.FinalEvaluation;
import com.resumebuilder.springaibackend.dto.InterviewTurnResponse.InterviewTurnScore;
import java.time.LocalDateTime;
import java.util.List;

public record InterviewSessionDetailResponse(
        String sessionId,
        String mode,
        String status,
        int durationMinutes,
        int elapsedSeconds,
        String memorySummary,
        FinalEvaluation finalEvaluation,
        ResumeSnapshot resumeSnapshot,
        List<InterviewMessageItem> messages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record InterviewMessageItem(
            String role,
            String content,
            InterviewTurnScore score
    ) {
    }
}
