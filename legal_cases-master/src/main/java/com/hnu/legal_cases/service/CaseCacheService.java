package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;

import java.util.List;
import java.util.Set;

public interface CaseCacheService {
    /**
     * 生成缓存key
     */
    String generateCacheKey(String keyword, String country, Integer period);

    /**
     * 生成缓存 key（含数据源筛选，sources 为空则与三参数版一致）
     */
    String generateCacheKey(String keyword, String country, Integer period, String sources);

    /**
     * 检查缓存是否存在且未过期
     */
    boolean hasCache(String cacheKey);

    /**
     * 获取缓存中的总数量
     */
    Long getCachedCount(String cacheKey);

    /**
     * 分页获取缓存的案例ID
     */
    Set<String> getCachedCaseIds(String cacheKey, int startIndex, int endIndex);

    /**
     * 将案例ID存入缓存
     */
    void cacheCaseIds(String cacheKey, List<CrawlerBaseInfoItem> items);

    /**
     * 删除缓存
     */
    void deleteCacheKey(String cacheKey);
}
