package com.hnu.legal_cases.service;

import java.util.concurrent.TimeUnit;

public interface OriginalDocumentCacheService {

    String getCachedText(String url);

    String fetchOrGet(String url, long timeout, TimeUnit unit);

    void prefetch(String url);

    void prefetch(String url, String sourceId, String title);

    /**
     * 预先保存一段兜底文本（例如 CourtListener 搜索接口返回的 snippet）。
     * 当详情抓取失败时，用这段文本展示/摘要，避免出现“有案例但正文完全为空”。
     */
    void cacheFallbackText(String url, String fallbackText);

    String getLastFailure(String url);

    void clearFailure(String url);
}
