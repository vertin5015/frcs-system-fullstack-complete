package com.hnu.legal_cases.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cloud embedding configuration. Kept explicit under {@code app.embedding} so the chat model and
 * embedding model can point to different providers (for example DeepSeek chat + Qwen embeddings).
 */
@Data
@ConfigurationProperties(prefix = "app.embedding")
public class EmbeddingProperties {

    private boolean enabled = false;

    private String apiKey;

    private String baseUrl;

    private String model;

    private String path = "/embeddings";

    private int timeoutMs = 30000;

    private Integer dimensions;
}
