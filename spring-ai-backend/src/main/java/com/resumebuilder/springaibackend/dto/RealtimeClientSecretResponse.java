package com.resumebuilder.springaibackend.dto;

public record RealtimeClientSecretResponse(
        String clientSecret,
        Long expiresAt,
        String model,
        String realtimeApiBaseUrl,
        String realtimeCallsPath
) {
}