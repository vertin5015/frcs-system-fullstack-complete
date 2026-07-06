package com.hnu.legal_cases.service.impl;

import cn.hutool.crypto.SecureUtil;
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
import java.util.List;
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
    /**
     * 缓存过期时间（单位：小时）
     */
    private static final long CACHE_EXPIRE_HOURS = 24;
    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

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

    /**
     * 删除缓存
     */
    @Override
    public void deleteCacheKey(String cacheKey) {
        try {
            stringRedisTemplate.delete(cacheKey);
            log.info("已经删除缓存key：{}", cacheKey);
        } catch (Exception e) {
            log.error("删除缓存key失败，key=" + cacheKey);
        }
    }


    /**
     * 将日期字符串转换为score（时间戳）
     */
    private double parseDateToScore(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return 0.0;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return date.toEpochDay();
        } catch (DateTimeParseException e) {
            log.warn("判决日期无法按 yyyy/MM/dd 解析，按最旧排序处理: {}", dateStr);
            return 0.0;
        }
    }
}
