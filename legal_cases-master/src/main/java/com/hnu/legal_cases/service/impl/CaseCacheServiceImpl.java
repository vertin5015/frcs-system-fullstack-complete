package com.hnu.legal_cases.service.impl;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hnu.legal_cases.dto.cases.SearchSourceStat;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.CaseCacheService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CaseCacheServiceImpl implements CaseCacheService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 缓存key前缀
     */
    private static final String CACHE_KEY_PREFIX = "case_search:";
    private static final String KEYWORD_KEY_PREFIX = "case_keywords:";
    /**
     * 缓存过期时间（单位：小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 24;
    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter SLASH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /**
     * 生成缓存key
     */
    @Override
    public String generateCacheKey(String keyword, String country, Integer period) {
        return generateCacheKey(keyword, country, period, null);
    }

    @Override
    public String generateCacheKey(String keyword, String country, Integer period, String sources) {
        String src = (sources == null || sources.isBlank()) ? "ALL" : sources.trim();
        String rawKey = keyword + ":" + country + ":" + period + ":" + src;
        String md5Key = SecureUtil.md5(rawKey);
        return CACHE_KEY_PREFIX + md5Key;
    }

    /**
     * 检查缓存是否存在且未过期
     */
    @Override
    public boolean hasCache(String cacheKey) {
        return stringRedisTemplate.hasKey(cacheKey);
    }

    /**
     * 获取缓存中的总数量
     */
    @Override
    public Long getCachedCount(String cacheKey) {
        try {
            return stringRedisTemplate.opsForZSet().zCard(cacheKey);
        } catch (Exception e) {
            log.error("获取缓存数量失败，key: {}，{}", cacheKey, e.getMessage());
            return 0L;
        }
    }

    /**
     * 分页获取缓存的案例ID
     */
    @Override
    public Set<String> getCachedCaseIds(String cacheKey, int startIndex, int endIndex) {
        try {
            // 按score倒序分页获取（最新的判决在前）
            return stringRedisTemplate.opsForZSet().reverseRange(cacheKey, startIndex, endIndex);
        } catch (Exception e) {
            throw new ServiceException("分页获取缓存失败，key=" + cacheKey + "，" + e.getMessage());
        }
    }

    /**
     * 将案例ID存入缓存
     */
    @Override
    public void cacheCaseIds(String cacheKey, List<CrawlerBaseInfoItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        try {
            ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();

            // 清空旧数据
            stringRedisTemplate.delete(cacheKey);

            // 添加caseId和score（案号 trim；日期解析失败时用 0 分，避免整批无法入缓存）
            for (CrawlerBaseInfoItem item : items) {
                String docket = item.getDocketNumber() == null ? "" : item.getDocketNumber().trim();
                if (StringUtils.isNotBlank(docket)) {
                    double score = this.parseDateToScore(item.getDateFiled());
                    zSetOps.add(cacheKey, docket, score);
                }
            }

            // 设置过期时间
            stringRedisTemplate.expire(cacheKey, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("缓存案例ID成功, key: {}, count: {}", cacheKey, this.getCachedCount(cacheKey));
        } catch (Exception e) {
            throw new ServiceException("缓存案例ID失败，key=" + cacheKey + "，" + e.getMessage());
        }
    }

    @Override
    public void cacheSourceStats(String cacheKey, List<SearchSourceStat> sourceStats) {
        if (sourceStats == null || sourceStats.isEmpty()) {
            return;
        }
        String statsKey = cacheKey + ":sourceStats";
        stringRedisTemplate.opsForValue().set(statsKey, JSON.toJSONString(sourceStats),
                CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    @Override
    public List<SearchSourceStat> getCachedSourceStats(String cacheKey) {
        String raw = stringRedisTemplate.opsForValue().get(cacheKey + ":sourceStats");
        if (StringUtils.isBlank(raw)) {
            return new ArrayList<>();
        }
        try {
            return JSON.parseObject(raw, new TypeReference<List<SearchSourceStat>>() {
            });
        } catch (Exception e) {
            log.warn("读取数据源统计缓存失败 key={} error={}", cacheKey, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void cacheCaseKeywords(String caseId, String keywordsZh, String keywordsEn) {
        if (StringUtils.isBlank(caseId)) {
            return;
        }
        try {
            String id = caseId.trim();
            if (StringUtils.isNotBlank(keywordsZh)) {
                stringRedisTemplate.opsForValue().set(KEYWORD_KEY_PREFIX + id + ":zh",
                        keywordsZh.trim(), 30, TimeUnit.DAYS);
            }
            if (StringUtils.isNotBlank(keywordsEn)) {
                stringRedisTemplate.opsForValue().set(KEYWORD_KEY_PREFIX + id + ":en",
                        keywordsEn.trim(), 30, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.warn("缓存案例关键词失败 caseId={} error={}", caseId, e.getMessage());
        }
    }

    @Override
    public Map<String, String> getCaseKeywords(Set<String> caseIds, String language) {
        Map<String, String> result = new LinkedHashMap<>();
        if (caseIds == null || caseIds.isEmpty()) {
            return result;
        }
        String lang = "zh".equalsIgnoreCase(language) ? "zh" : "en";
        List<String> ids = new ArrayList<>(caseIds);
        List<String> keys = new ArrayList<>(ids.size());
        for (String id : ids) {
            keys.add(KEYWORD_KEY_PREFIX + id + ":" + lang);
        }
        try {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values == null) {
                return result;
            }
            for (int i = 0; i < ids.size() && i < values.size(); i++) {
                String value = values.get(i);
                if (StringUtils.isNotBlank(value)) {
                    result.put(ids.get(i), value);
                }
            }
        } catch (Exception e) {
            log.warn("读取案例关键词失败 language={} error={}", language, e.getMessage());
        }
        return result;
    }

    /**
     * 删除缓存
     */
    @Override
    public void deleteCacheKey(String cacheKey) {
        try {
            stringRedisTemplate.delete(cacheKey);
            stringRedisTemplate.delete(cacheKey + ":sourceStats");
            log.info("已经删除缓存key：{}", cacheKey);
        } catch (Exception e) {
            log.error("删除缓存key失败，key=" + cacheKey);
        }
    }


    /**
     * 将日期字符串转换为score（时间戳）
     */
    static double parseDateToScore(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return 0.0;
        }

        try {
            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
                date = LocalDate.parse(dateStr, SLASH_DATE_FORMATTER);
            }
            return date.toEpochDay();
        } catch (DateTimeParseException e) {
            log.warn("判决日期无法解析，按最旧排序处理: {}", dateStr);
            return 0.0;
        }
    }
}
