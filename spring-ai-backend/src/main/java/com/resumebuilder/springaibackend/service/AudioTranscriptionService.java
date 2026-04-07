package com.resumebuilder.springaibackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumebuilder.springaibackend.dto.AudioTranscriptionResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AudioTranscriptionService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String speechApiKey;
    private final String transcriptionsPath;
    private final String defaultModel;

    public AudioTranscriptionService(
            ObjectMapper objectMapper,
            @Value("${OPENAI_SPEECH_BASE_URL:${spring.ai.openai.speech.base-url:${spring.ai.openai.base-url:https://api.openai.com}}}") String speechBaseUrl,
            @Value("${OPENAI_SPEECH_API_KEY:${spring.ai.openai.speech.api-key:${spring.ai.openai.api-key:}}}") String speechApiKey,
            @Value("${OPENAI_SPEECH_TRANSCRIPTIONS_PATH:/v1/audio/transcriptions}") String transcriptionsPath,
            @Value("${OPENAI_SPEECH_TRANSCRIPTION_MODEL:gpt-4o-mini-transcribe}") String defaultModel
    ) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(normalizeBaseUrl(speechBaseUrl)).build();
        this.speechApiKey = speechApiKey;
        this.transcriptionsPath = normalizePath(transcriptionsPath);
        this.defaultModel = defaultModel;
    }

    public AudioTranscriptionResponse transcribe(MultipartFile file, String model, String language, String prompt) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("audio file cannot be empty");
        }
        if (!StringUtils.hasText(this.speechApiKey)) {
            throw new IllegalStateException("Speech API key is not configured");
        }

        String targetModel = StringUtils.hasText(model) ? model.trim() : this.defaultModel;
        byte[] bytes = readBytes(file);

        ByteArrayResource fileResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                String name = file.getOriginalFilename();
                return StringUtils.hasText(name) ? name : "audio.webm";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("model", targetModel);
        if (StringUtils.hasText(language)) {
            body.add("language", language.trim());
        }
        if (StringUtils.hasText(prompt)) {
            body.add("prompt", prompt.trim());
        }

        String rawResponse = restClient.post()
                .uri(this.transcriptionsPath)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + this.speechApiKey.trim())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(String.class);

        String text = extractTranscriptionText(rawResponse);
        if (!StringUtils.hasText(text)) {
            throw new IllegalStateException("Speech provider returned empty transcription");
        }
        return new AudioTranscriptionResponse(text);
    }

    private String extractTranscriptionText(String rawResponse) {
        if (!StringUtils.hasText(rawResponse)) {
            return "";
        }
        String content = rawResponse.trim();
        try {
            JsonNode root = objectMapper.readTree(content);
            String text = root.path("text").asText("").trim();
            if (StringUtils.hasText(text)) {
                return text;
            }
            String transcript = root.path("transcript").asText("").trim();
            if (StringUtils.hasText(transcript)) {
                return transcript;
            }
            String result = root.path("result").asText("").trim();
            if (StringUtils.hasText(result)) {
                return result;
            }
        } catch (Exception ignore) {
            // Fallback to raw text when provider returns non-standard payload.
        }
        return content;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read audio file", ex);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://api.openai.com";
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/v1/audio/transcriptions";
        }
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}