package com.hnu.legal_cases.service;

/**
 * Text-to-vector abstraction. Implementations may use a cloud embedding model or a local fallback.
 */
public interface EmbeddingService {
    float[] embed(String text);
}
