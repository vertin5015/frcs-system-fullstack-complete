package com.hnu.legal_cases.service;

import java.util.concurrent.TimeUnit;

public interface OriginalDocumentCacheService {

    String getCachedText(String url);

    String fetchOrGet(String url, long timeout, TimeUnit unit);

    void prefetch(String url);

    void prefetch(String url, String sourceId, String title);

    String getLastFailure(String url);

    void clearFailure(String url);
}
