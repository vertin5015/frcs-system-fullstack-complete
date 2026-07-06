package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.service.LocalEmbeddingService;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 本地轻量 embedding（哈希向量），避免外部 embedding 通道不可用导致入库/检索阻塞。
 * 说明：这是工程兜底方案，不依赖外部模型服务；语义效果弱于专业 embedding 模型，但稳定可用。
 */
@Service
public class LocalEmbeddingServiceImpl implements LocalEmbeddingService {
    private static final int DIM = 384;

    @Override
    public float[] embed(String text) {
        float[] v = new float[DIM];
        if (StringUtils.isBlank(text)) {
            return v;
        }
        String normalized = text.trim().toLowerCase();
        byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0) {
            return v;
        }
        // 3-gram hashing trick
        for (int i = 0; i < bytes.length; i++) {
            int b1 = bytes[i] & 0xff;
            int b2 = (i + 1 < bytes.length) ? (bytes[i + 1] & 0xff) : 0;
            int b3 = (i + 2 < bytes.length) ? (bytes[i + 2] & 0xff) : 0;
            int h = mixHash(b1, b2, b3);
            int idx = Math.floorMod(h, DIM);
            v[idx] += 1.0f;
        }
        l2Normalize(v);
        return v;
    }

    private static int mixHash(int a, int b, int c) {
        int h = 0x811C9DC5;
        h ^= a;
        h *= 16777619;
        h ^= b;
        h *= 16777619;
        h ^= c;
        h *= 16777619;
        return h;
    }

    private static void l2Normalize(float[] v) {
        double sum = 0.0;
        for (float x : v) {
            sum += x * x;
        }
        double norm = Math.sqrt(sum);
        if (norm <= 1e-9) {
            Arrays.fill(v, 0f);
            return;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] = (float) (v[i] / norm);
        }
    }
}
