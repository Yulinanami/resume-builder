package com.resumebuilder.springaibackend.dto;

import java.util.List;

public record InterviewTurnResponse(
        String assistantReply,
        String phase,
        String nextAction,
        InterviewTurnScore turnScore,
        FinalEvaluation finalEvaluation,
        String memorySummary,
        String sessionId
) {

    public record InterviewTurnScore(int score, String comment) {
    }

    public record FinalEvaluation(
            int projectScore,
            int skillScore,
            int workScore,
            int educationScore,
            int totalScore,
            boolean passed,
            String summary,
            List<String> improvements
    ) {
    }
}

