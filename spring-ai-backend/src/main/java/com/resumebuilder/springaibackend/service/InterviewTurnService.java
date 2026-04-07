package com.resumebuilder.springaibackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumebuilder.springaibackend.dto.InterviewStreamEvent;
import com.resumebuilder.springaibackend.dto.InterviewTurnRequest;
import com.resumebuilder.springaibackend.dto.InterviewTurnRequest.InterviewHistoryItem;
import com.resumebuilder.springaibackend.dto.InterviewTurnRequest.ResumeSnapshot;
import com.resumebuilder.springaibackend.dto.InterviewTurnResponse;
import com.resumebuilder.springaibackend.dto.InterviewTurnResponse.FinalEvaluation;
import com.resumebuilder.springaibackend.dto.InterviewTurnResponse.InterviewTurnScore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class InterviewTurnService {

    private static final Pattern PARTIAL_ASSISTANT_REPLY = Pattern.compile("\\\"assistantReply\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)");

    private static final String CANDIDATE_SYSTEM_PROMPT = """
            你是一名专业、严格、但表达友善的技术面试官。
            当前模式：用户是候选人，你负责提问、追问和评分。
            规则：
            1) 第一轮仅允许候选人进行 1-2 分钟自我介绍。
            2) 每一轮只提出 1 个主问题或 1 个追问。
            3) 必须基于简历内容提问，不得脱离简历。
            4) 每轮输出都要维护 memorySummary（不超过 220 中文字符）。
            5) 只输出一个 JSON 对象，不要 markdown，不要额外解释。
            6) assistantReply 必须是 JSON 的第一个字段。
            7) 全程使用中文。
            JSON 结构：
            {
              "assistantReply": "string",
              "phase": "opening|skills|work|projects|scenario|written|summary",
              "nextAction": "continue|finish",
              "turnScore": {"score": number, "comment": "string"} | null,
              "memorySummary": "string",
              "finalEvaluation": {
                "projectScore": number,
                "skillScore": number,
                "workScore": number,
                "educationScore": number,
                "totalScore": number,
                "passed": boolean,
                "summary": "string",
                "improvements": ["string"]
              } | null
            }
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final InterviewSessionStoreService interviewSessionStoreService;

    public InterviewTurnService(
            ChatClient chatClient,
            ObjectMapper objectMapper,
            InterviewSessionStoreService interviewSessionStoreService
    ) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.interviewSessionStoreService = interviewSessionStoreService;
    }

    public Flux<InterviewStreamEvent> handleStream(InterviewTurnRequest request) {
        Sinks.Many<InterviewStreamEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuilder rawBuilder = new StringBuilder();
        StringBuilder lastAssistantReply = new StringBuilder();
        String sessionId = resolveSessionId(request);

        Disposable disposable = chatClient.prompt(new Prompt(buildMessages(request))).stream().content()
                .subscribe(
                        chunk -> {
                            if (chunk == null || chunk.isEmpty()) {
                                return;
                            }
                            rawBuilder.append(chunk);
                            String partial = extractAssistantReplyFromPartialJson(rawBuilder.toString());
                            if (partial != null && !partial.isBlank() && !partial.equals(lastAssistantReply.toString())) {
                                lastAssistantReply.setLength(0);
                                lastAssistantReply.append(partial);
                                sink.tryEmitNext(new InterviewStreamEvent("chunk", partial));
                            }
                        },
                        error -> {
                            sink.tryEmitNext(new InterviewStreamEvent("error", safeText(error.getMessage())));
                            sink.tryEmitComplete();
                        },
                        () -> {
                            InterviewTurnResponse normalized = normalizeTurnResponse(rawBuilder.toString());
                            InterviewTurnResponse finalResponse = attachSessionId(normalized, sessionId);
                            try {
                                interviewSessionStoreService.saveTurn(sessionId, request, finalResponse);
                            } catch (Exception ex) {
                                sink.tryEmitNext(new InterviewStreamEvent("error", safeText(ex.getMessage())));
                                sink.tryEmitComplete();
                                return;
                            }

                            String finalAssistantReply = safeText(finalResponse.assistantReply());
                            if (!finalAssistantReply.isBlank() && !finalAssistantReply.equals(lastAssistantReply.toString())) {
                                sink.tryEmitNext(new InterviewStreamEvent("chunk", finalAssistantReply));
                            }
                            sink.tryEmitNext(new InterviewStreamEvent("done", toJson(finalResponse)));
                            sink.tryEmitComplete();
                        }
                );

        return sink.asFlux().doOnCancel(disposable::dispose);
    }

    private List<Message> buildMessages(InterviewTurnRequest request) {
        String mode = normalizeMode(request.mode());
        String command = normalizeCommand(request.command());

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt(mode, extractJobTitle(request.resumeSnapshot()))));

        List<InterviewHistoryItem> history = request.history() == null ? List.of() : request.history();
        int window = "start".equals(command) ? 4 : 8;
        int from = Math.max(0, history.size() - window);
        for (InterviewHistoryItem item : history.subList(from, history.size())) {
            if (item == null) {
                continue;
            }
            String role = safeText(item.role()).toLowerCase(Locale.ROOT);
            String content = truncateText(safeText(item.content()), 700);
            if (content.isBlank()) {
                continue;
            }
            if ("assistant".equals(role)) {
                messages.add(new AssistantMessage(content));
            } else {
                messages.add(new UserMessage(content));
            }
        }

        messages.add(new UserMessage(buildUserPrompt(request, mode, command)));
        return messages;
    }

    private String systemPrompt(String mode, String jobTitle) {
        if ("interviewer".equals(mode)) {
            String role = jobTitle.isBlank() ? "技术岗位" : jobTitle;
            return """
                    你在一场模拟面试中扮演候选人。
                    当前模式：用户是面试官，你是候选人。
                    回答必须与简历事实保持一致，不能编造与简历冲突的信息。
                    每轮都要维护 memorySummary（不超过 220 中文字符）。
                    只输出一个 JSON 对象，不要 markdown，不要额外解释。
                    assistantReply 必须是 JSON 的第一个字段。
                    全程使用中文。
                    候选人目标岗位：""" + role + "\n"
                    + "JSON 结构：\n"
                    + "{\n"
                    + "  \"assistantReply\": \"string\",\n"
                    + "  \"phase\": \"opening|skills|work|projects|scenario|written|summary\",\n"
                    + "  \"nextAction\": \"continue|finish\",\n"
                    + "  \"turnScore\": null,\n"
                    + "  \"memorySummary\": \"string\",\n"
                    + "  \"finalEvaluation\": {\n"
                    + "    \"projectScore\": number,\n"
                    + "    \"skillScore\": number,\n"
                    + "    \"workScore\": number,\n"
                    + "    \"educationScore\": number,\n"
                    + "    \"totalScore\": number,\n"
                    + "    \"passed\": boolean,\n"
                    + "    \"summary\": \"string\",\n"
                    + "    \"improvements\": [\"string\"]\n"
                    + "  } | null\n"
                    + "}";
        }
        return CANDIDATE_SYSTEM_PROMPT;
    }

    private String buildUserPrompt(InterviewTurnRequest request, String mode, String command) {
        int durationMinutes = request.durationMinutes() == null || request.durationMinutes() <= 0 ? 60 : request.durationMinutes();
        int elapsedSeconds = request.elapsedSeconds() == null || request.elapsedSeconds() < 0 ? 0 : request.elapsedSeconds();
        int elapsedMin = elapsedSeconds / 60;
        int remainMin = Math.max(durationMinutes - elapsedMin, 0);

        String commandLine;
        if ("start".equals(command)) {
            commandLine = "candidate".equals(mode)
                    ? "现在开始面试。先请候选人进行 1-2 分钟自我介绍。"
                    : "现在开始面试。先给出简短开场，然后等待面试官提问。";
        } else if ("finish".equals(command)) {
            commandLine = "现在结束面试，并给出最终评估。";
        } else {
            commandLine = "本轮用户输入：" + (safeText(request.userInput()).isBlank() ? "（空）" : safeText(request.userInput()));
        }

        return String.join("\n",
                "角色关系：" + ("candidate".equals(mode) ? "你是面试官" : "你是候选人"),
                "目标时长：" + durationMinutes + " 分钟，已用：" + elapsedMin + " 分钟，剩余：" + remainMin + " 分钟。",
                "命令：" + command,
                commandLine,
                "",
                "记忆摘要（优先参考）：",
                normalizeMemorySummaryText(safeText(request.memorySummary())).isBlank()
                        ? "（无）"
                        : normalizeMemorySummaryText(safeText(request.memorySummary())),
                "",
                "简历快照：",
                buildResumeDigest(request.resumeSnapshot()),
                "",
                "请仅输出一个 JSON 对象。"
        );
    }

    private String buildResumeDigest(ResumeSnapshot snapshot) {
        if (snapshot == null) {
            return "空";
        }
        StringBuilder sb = new StringBuilder();
        if (snapshot.basicInfo() != null) {
            sb.append("基础信息=")
                    .append("姓名:").append(safeText(snapshot.basicInfo().name())).append(",")
                    .append("岗位:").append(safeText(snapshot.basicInfo().jobTitle())).append(",")
                    .append("工作年限:").append(safeText(snapshot.basicInfo().workYears())).append(",")
                    .append("学历:").append(safeText(snapshot.basicInfo().educationLevel())).append("\n");
        }
        sb.append("技能=").append(truncateText(toPlainText(snapshot.skillsText()), 300)).append("\n");
        sb.append("工作经历数量=").append(snapshot.workList() == null ? 0 : snapshot.workList().size()).append("\n");
        sb.append("项目经历数量=").append(snapshot.projectList() == null ? 0 : snapshot.projectList().size()).append("\n");
        sb.append("教育经历数量=").append(snapshot.educationList() == null ? 0 : snapshot.educationList().size()).append("\n");
        if (!safeText(snapshot.selfIntro()).isBlank()) {
            sb.append("自我介绍=").append(truncateText(toPlainText(snapshot.selfIntro()), 300)).append("\n");
        }
        return sb.toString().trim();
    }

    private InterviewTurnResponse normalizeTurnResponse(String rawContent) {
        InterviewTurnResponse fallback = new InterviewTurnResponse(
                safeText(rawContent).isBlank() ? "本轮回答为空，请重试。" : safeText(rawContent),
                "opening",
                "continue",
                null,
                null,
                "",
                ""
        );

        try {
            JsonNode root = objectMapper.readTree(extractJsonObject(rawContent));

            String assistantReply = safeText(root.path("assistantReply").asText(fallback.assistantReply()));
            if (assistantReply.isBlank()) {
                assistantReply = fallback.assistantReply();
            }

            String phase = safeText(root.path("phase").asText("opening"));
            if (phase.isBlank()) {
                phase = "opening";
            }

            String nextAction = "finish".equalsIgnoreCase(safeText(root.path("nextAction").asText())) ? "finish" : "continue";

            InterviewTurnScore turnScore = null;
            JsonNode turnScoreNode = root.path("turnScore");
            if (turnScoreNode.isObject()) {
                turnScore = new InterviewTurnScore(
                        clampScore(turnScoreNode.path("score")),
                        safeText(turnScoreNode.path("comment").asText())
                );
            }

            FinalEvaluation finalEvaluation = null;
            JsonNode finalNode = root.path("finalEvaluation");
            if (finalNode.isObject()) {
                List<String> improvements = new ArrayList<>();
                JsonNode improvementsNode = finalNode.path("improvements");
                if (improvementsNode.isArray()) {
                    improvementsNode.forEach(item -> {
                        String value = safeText(item.asText());
                        if (!value.isBlank()) {
                            improvements.add(value);
                        }
                    });
                }

                finalEvaluation = new FinalEvaluation(
                        clampScore(finalNode.path("projectScore")),
                        clampScore(finalNode.path("skillScore")),
                        clampScore(finalNode.path("workScore")),
                        clampScore(finalNode.path("educationScore")),
                        clampScore(finalNode.path("totalScore")),
                        finalNode.path("passed").asBoolean(false),
                        safeText(finalNode.path("summary").asText()),
                        improvements
                );
            }

            return new InterviewTurnResponse(
                    assistantReply,
                    phase,
                    nextAction,
                    turnScore,
                    finalEvaluation,
                    normalizeMemorySummaryText(safeText(root.path("memorySummary").asText())),
                    safeText(root.path("sessionId").asText())
            );
        } catch (Exception ignore) {
            String partial = extractAssistantReplyFromPartialJson(rawContent);
            String assistantReply = partial == null ? fallback.assistantReply() : partial;
            return new InterviewTurnResponse(assistantReply, fallback.phase(), fallback.nextAction(), null, null, "", "");
        }
    }

    private String toJson(InterviewTurnResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            return "{\"assistantReply\":\"" + escapeJson(response.assistantReply())
                    + "\",\"phase\":\"" + escapeJson(response.phase())
                    + "\",\"nextAction\":\"" + escapeJson(response.nextAction())
                    + "\",\"turnScore\":null,\"finalEvaluation\":null,\"memorySummary\":\"\",\"sessionId\":\"\"}";
        }
    }

    private String escapeJson(String value) {
        return safeText(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractJsonObject(String raw) {
        String text = safeText(raw);
        Matcher fenced = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```").matcher(text);
        if (fenced.find()) {
            return safeText(fenced.group(1));
        }

        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return text.substring(first, last + 1);
        }
        return text;
    }

    private String extractAssistantReplyFromPartialJson(String raw) {
        Matcher matcher = PARTIAL_ASSISTANT_REPLY.matcher(safeText(raw));
        if (!matcher.find()) {
            return null;
        }
        return decodePartialJsonString(matcher.group(1));
    }

    private String decodePartialJsonString(String raw) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch != '\\') {
                output.append(ch);
                continue;
            }
            if (i + 1 >= raw.length()) {
                break;
            }
            char next = raw.charAt(i + 1);
            switch (next) {
                case 'n' -> {
                    output.append('\n');
                    i++;
                }
                case 'r' -> {
                    output.append('\r');
                    i++;
                }
                case 't' -> {
                    output.append('\t');
                    i++;
                }
                case '"', '\\', '/' -> {
                    output.append(next);
                    i++;
                }
                default -> {
                    output.append(next);
                    i++;
                }
            }
        }
        return output.toString();
    }

    private int clampScore(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return 0;
        }
        return Math.max(0, Math.min(100, node.asInt()));
    }

    private String normalizeMode(String mode) {
        return "interviewer".equalsIgnoreCase(safeText(mode)) ? "interviewer" : "candidate";
    }

    private String normalizeCommand(String command) {
        String normalized = safeText(command).toLowerCase(Locale.ROOT);
        if ("start".equals(normalized) || "finish".equals(normalized)) {
            return normalized;
        }
        return "continue";
    }

    private String resolveSessionId(InterviewTurnRequest request) {
        String command = normalizeCommand(request.command());
        if ("start".equals(command)) {
            return UUID.randomUUID().toString();
        }

        String sessionId = safeText(request.sessionId());
        if (!sessionId.isBlank()) {
            return sessionId;
        }
        return UUID.randomUUID().toString();
    }

    private InterviewTurnResponse attachSessionId(InterviewTurnResponse response, String sessionId) {
        return new InterviewTurnResponse(
                response.assistantReply(),
                response.phase(),
                response.nextAction(),
                response.turnScore(),
                response.finalEvaluation(),
                response.memorySummary(),
                safeText(sessionId)
        );
    }

    private String extractJobTitle(ResumeSnapshot snapshot) {
        if (snapshot == null || snapshot.basicInfo() == null) {
            return "";
        }
        return safeText(snapshot.basicInfo().jobTitle());
    }

    private String toPlainText(String text) {
        return safeText(text)
                .replaceAll("(?i)<br\\s*/?>", "\\n")
                .replaceAll("(?i)</?(p|div|li|ul|ol|h[1-6])[^>]*>", "\\n")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("\\n{3,}", "\\n\\n")
                .trim();
    }

    private String truncateText(String text, int maxLen) {
        String value = safeText(text);
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    private String normalizeMemorySummaryText(String content) {
        return truncateText(safeText(content).replaceAll("\\s+", " "), 600);
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}