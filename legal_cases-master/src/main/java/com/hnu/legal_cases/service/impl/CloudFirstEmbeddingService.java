package com.hnu.legal_cases.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hnu.legal_cases.config.EmbeddingProperties;
import com.hnu.legal_cases.service.EmbeddingService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Cloud-first embedding service.
 *
 * <p>Uses an OpenAI-compatible embedding endpoint (for example a Qwen embedding model through
 * DashScope compatible mode) when configured, and automatically falls back to the local hash
 * vector implementation when the endpoint is disabled, missing, or unavailable.</p>
 */
@Slf4j
@Service
@Primary
public class CloudFirstEmbeddingService implements EmbeddingService {

    private static final int MAX_CACHE_SIZE = 10_000;

    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final EmbeddingService localEmbeddingService;
    private final ConcurrentHashMap<String, float[]> cache = new ConcurrentHashMap<>();

    public CloudFirstEmbeddingService(
            EmbeddingProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("embeddingRestTemplate") RestTemplate restTemplate,
            @Qualifier("localEmbeddingServiceImpl") EmbeddingService localEmbeddingService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.localEmbeddingService = localEmbeddingService;
    }

    @Override
    public float[] embed(String text) {
        if (StringUtils.isBlank(text)) {
            return localEmbeddingService.embed(text);
        }

        float[] cached = cache.get(text);
        if (cached != null) {
            return (float[]) cached.clone();
        }

        float[] vector = tryCloudEmbed(text);
        if (vector == null) {
            vector = localEmbeddingService.embed(text);
        } else {
            normalize(vector);
            if (cache.size() >= MAX_CACHE_SIZE) {
                cache.clear();
            }
            cache.put(text, vector);
            return (float[]) vector.clone();
        }
        return vector;
    }

    private float[] tryCloudEmbed(String text) {
        if (!properties.isEnabled()
                || StringUtils.isBlank(properties.getApiKey())
                || StringUtils.isBlank(properties.getBaseUrl())
                || StringUtils.isBlank(properties.getModel())) {
            log.debug("云端 embedding 未配置，使用本地哈希向量兜底");
            return null;
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", properties.getModel());
            body.put("input", text);
            if (properties.getDimensions() != null && properties.getDimensions() > 0) {
                body.put("dimensions", properties.getDimensions());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getApiKey());
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            String base = properties.getBaseUrl().trim().replaceAll("/+$", "");
            String path = properties.getPath() == null || properties.getPath().isBlank()
                    ? "/embeddings"
                    : properties.getPath().trim();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            ResponseEntity<String> response = restTemplate.postForEntity(base + path, request, String.class);
            if (response.getStatusCode().is2xxSuccessful() && StringUtils.isNotBlank(response.getBody())) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");
                if (data.isArray() && data.size() > 0) {
                    JsonNode embedding = data.get(0).path("embedding");
                    if (embedding.isArray() && embedding.size() > 0) {
                        float[] out = new float[embedding.size()];
                        for (int i = 0; i < embedding.size(); i++) {
                            out[i] = (float) embedding.get(i).asDouble();
                        }
                        log.debug("云端 embedding 成功 model={} dims={}", properties.getModel(), out.length);
                        return out;
                    }
                }
            }
            log.warn("云端 embedding 返回格式异常 status={} body={}",
                    response.getStatusCode(), response.getBody());
            return null;
        } catch (Exception e) {
            log.warn("云端 embedding 调用失败，使用本地哈希向量兜底: {}", e.getMessage());
            return null;
        }
    }

    private static void normalize(float[] vector) {
        double sum = 0.0;
        for (float v : vector) {
            sum += (double) v * v;
        }
        double norm = Math.sqrt(sum);
        if (norm <= 1e-9) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
    }
}
