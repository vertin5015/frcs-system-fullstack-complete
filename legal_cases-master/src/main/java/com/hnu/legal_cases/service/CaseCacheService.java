package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.dto.cases.SearchSourceStat;

import java.util.List;
import java.util.Map;
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
     * 缓存每个数据源的结果统计，供缓存命中时同样展示给用户。
     */
    void cacheSourceStats(String cacheKey, List<SearchSourceStat> sourceStats);

    /**
     * 读取缓存中的数据源统计。
     */
    List<SearchSourceStat> getCachedSourceStats(String cacheKey);

    /**
     * 保存案例 AI 摘要中提取出的关键词（中文/英文各一份）。
     */
    void cacheCaseKeywords(String caseId, String keywordsZh, String keywordsEn);

    /**
     * 按语言批量读取案例关键词：返回 caseId -> 关键词。
     */
    Map<String, String> getCaseKeywords(Set<String> caseIds, String language);

    /**
     * 缓存搜索结果卡片摘要的翻译结果。
     */
    void cacheCaseSummary(String caseId, String language, String summary);

    /**
     * 批量读取指定语言的卡片摘要翻译。
     */
    Map<String, String> getCaseSummaries(Set<String> caseIds, String language);

    /**
     * 删除缓存
     */
    void deleteCacheKey(String cacheKey);
}
