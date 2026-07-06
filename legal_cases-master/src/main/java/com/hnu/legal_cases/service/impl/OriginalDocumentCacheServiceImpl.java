package com.hnu.legal_cases.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hnu.legal_cases.service.CrawlerService;
import com.hnu.legal_cases.service.LocalKbService;
import com.hnu.legal_cases.service.OriginalDocumentCacheService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OriginalDocumentCacheServiceImpl implements OriginalDocumentCacheService {

    private static final Path CACHE_FILE = Path.of("original-cache", "original-text-cache.json");

    private final Map<String, String> textCache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> tasks = new ConcurrentHashMap<>();
    private final Map<String, String> failures = new ConcurrentHashMap<>();
    private final Map<String, CaseDocumentMeta> metadata = new ConcurrentHashMap<>();

    @Autowired
    private CrawlerService crawlerService;
    @Autowired
    private LocalKbService localKbService;
    @Autowired
    @Qualifier("crawlerTaskExecutor")
    private TaskExecutor caseTaskExecutor;

    @PostConstruct
    public void loadPersistedCache() {
        try {
            if (!Files.exists(CACHE_FILE)) {
                return;
            }
            String json = Files.readString(CACHE_FILE, StandardCharsets.UTF_8);
            if (StringUtils.isBlank(json)) {
                return;
            }
            Map<String, String> persisted = JSON.parseObject(json, new TypeReference<Map<String, String>>() {
            });
            if (persisted != null && !persisted.isEmpty()) {
                textCache.putAll(persisted);
                log.info("原文正文持久缓存加载完成 count={}", persisted.size());
            }
        } catch (Exception e) {
            log.warn("原文正文持久缓存加载失败: {}", e.getMessage());
        }
    }

    @Override
    public String getCachedText(String url) {
        String key = normalize(url);
        return StringUtils.isBlank(key) ? null : textCache.get(key);
    }

    @Override
    public String fetchOrGet(String url, long timeout, TimeUnit unit) {
        String key = normalize(url);
        if (StringUtils.isBlank(key)) {
            return null;
        }
        String cached = textCache.get(key);
        if (StringUtils.isNotBlank(cached)) {
            return cached;
        }
        CompletableFuture<String> task = tasks.computeIfAbsent(key, this::startFetchTask);
        try {
            String text = task.get(timeout, unit);
            return StringUtils.isBlank(text) ? null : text;
        } catch (Exception e) {
            log.warn("原文正文缓存等待超时或失败 url={} msg={}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public void prefetch(String url) {
        prefetch(url, null, null);
    }

    @Override
    public void prefetch(String url, String sourceId, String title) {
        String key = normalize(url);
        if (StringUtils.isBlank(key) || StringUtils.isNotBlank(textCache.get(key)) || failures.containsKey(key)) {
            return;
        }
        rememberMeta(key, sourceId, title);
        tasks.computeIfAbsent(key, this::startFetchTask);
    }

    @Override
    public String getLastFailure(String url) {
        String key = normalize(url);
        return StringUtils.isBlank(key) ? null : failures.get(key);
    }

    @Override
    public void clearFailure(String url) {
        String key = normalize(url);
        if (StringUtils.isNotBlank(key)) {
            failures.remove(key);
        }
    }

    private CompletableFuture<String> startFetchTask(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String text = crawlerService.getCaseDetail(url);
                if (StringUtils.isNotBlank(text)) {
                    textCache.put(url, text);
                    failures.remove(url);
                    persistCacheQuietly();
                    CompletableFuture.runAsync(() -> ingestFullTextToKb(url, text), caseTaskExecutor);
                    log.info("原文正文缓存完成 url={} chars={}", url, text.length());
                    return text;
                }
                failures.put(url, "原文站点暂未返回可读正文");
            } catch (Exception e) {
                failures.put(url, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                log.warn("原文正文缓存抓取失败 url={} msg={}", url, e.getMessage());
            } finally {
                tasks.remove(url);
            }
            return null;
        }, caseTaskExecutor);
    }

    private String normalize(String url) {
        return url == null ? null : url.trim();
    }

    private void rememberMeta(String url, String sourceId, String title) {
        if (StringUtils.isBlank(sourceId) && StringUtils.isBlank(title)) {
            return;
        }
        CaseDocumentMeta old = metadata.get(url);
        CaseDocumentMeta meta = old == null ? new CaseDocumentMeta() : old;
        if (StringUtils.isNotBlank(sourceId)) {
            meta.sourceId = sourceId.trim();
        }
        if (StringUtils.isNotBlank(title)) {
            meta.title = title.trim();
        }
        metadata.put(url, meta);
    }

    private void ingestFullTextToKb(String url, String text) {
        try {
            CaseDocumentMeta meta = metadata.get(url);
            String sourceId = meta != null && StringUtils.isNotBlank(meta.sourceId) ? meta.sourceId : url;
            String title = meta != null && StringUtils.isNotBlank(meta.title) ? meta.title : url;
            int chunks = localKbService.ingestCaseDocument(sourceId, title, text);
            log.info("原文全文同步知识库完成 sourceId={} chunks={}", sourceId, chunks);
        } catch (Exception e) {
            log.warn("原文全文同步知识库失败 url={} msg={}", url, e.getMessage());
        }
    }

    private synchronized void persistCacheQuietly() {
        try {
            Files.createDirectories(CACHE_FILE.getParent());
            Files.writeString(CACHE_FILE, JSON.toJSONString(textCache), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("原文正文持久缓存写入失败: {}", e.getMessage());
        }
    }

    private static class CaseDocumentMeta {
        private String sourceId;
        private String title;
    }
}
