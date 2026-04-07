package com.resumebuilder.springaibackend.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final List<String> corsAllowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

    private int ragTopK = 5;

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public int getRagTopK() {
        return ragTopK;
    }

    public void setRagTopK(int ragTopK) {
        this.ragTopK = ragTopK;
    }
}
