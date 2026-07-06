package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dao.UsageLogMapper;
import com.hnu.legal_cases.dao.UserMapper;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.UsageLog;
import com.hnu.legal_cases.service.SummaryQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SummaryQuotaServiceImpl implements SummaryQuotaService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UsageLogMapper usageLogMapper;
    private final UserMapper userMapper;

    @Value("${app.quota.summary-daily-per-user:50}")
    private int summaryDailyPerUser;
    @Value("${app.quota.enabled:false}")
    private boolean quotaEnabled;
    /** Comma-separated user ids: no daily Redis cap and no summary-credit debit/refund. */
    @Value("${app.quota.batch-user-ids:}")
    private String batchUserIdsRaw;

    private Set<Long> batchUserIds = Collections.emptySet();

    @PostConstruct
    void parseBatchUserIds() {
        if (batchUserIdsRaw == null || batchUserIdsRaw.isBlank()) {
            batchUserIds = Collections.emptySet();
            return;
        }
        Set<Long> ids = new HashSet<>();
        for (String part : batchUserIdsRaw.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            ids.add(Long.parseLong(s));
        }
        batchUserIds = Collections.unmodifiableSet(ids);
    }

    private boolean isBatchUser(Long userId) {
        return userId != null && batchUserIds.contains(userId);
    }

    @Override
    public void tryAcquireSummaryQuota(Long userId, String caseId) {
        tryAcquireSummaryQuota(userId, caseId, "SUMMARY_ASYNC");
    }

    @Override
    public void tryAcquireSummaryQuota(Long userId, String caseId, String usageActionType) {
        if (!quotaEnabled) {
            return;
        }
        if (userId == null || userId == 0L) {
            return;
        }
        if (!isBatchUser(userId)) {
            int rows = userMapper.decrementSummaryCredits(userId);
            if (rows == 0) {
                throw new ServiceException("QUOTA_EXCEEDED: 摘要剩余次数已用完，请购买次数后再试");
            }
            String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String key = "quota:summary:" + userId + ":" + day;
            Long n = stringRedisTemplate.opsForValue().increment(key);
            if (n != null && n == 1L) {
                stringRedisTemplate.expire(key, 48, TimeUnit.HOURS);
            }
            if (n != null && n > summaryDailyPerUser) {
                stringRedisTemplate.opsForValue().decrement(key);
                userMapper.incrementSummaryCredits(userId);
                throw new ServiceException("QUOTA_EXCEEDED: 今日异步摘要次数已达上限");
            }
        }
        UsageLog row = new UsageLog();
        row.setUserId(userId);
        row.setCaseId(caseId);
        row.setActionType(usageActionType != null ? usageActionType : "SUMMARY_ASYNC");
        usageLogMapper.insert(row);
    }

    @Override
    public void refundSummaryQuota(Long userId, String caseId) {
        if (!quotaEnabled) {
            return;
        }
        if (userId == null || userId == 0L) {
            return;
        }
        if (isBatchUser(userId)) {
            return;
        }
        userMapper.incrementSummaryCredits(userId);
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = "quota:summary:" + userId + ":" + day;
        Long after = stringRedisTemplate.opsForValue().decrement(key);
        if (after != null && after < 0) {
            stringRedisTemplate.opsForValue().increment(key);
        }
    }
}
