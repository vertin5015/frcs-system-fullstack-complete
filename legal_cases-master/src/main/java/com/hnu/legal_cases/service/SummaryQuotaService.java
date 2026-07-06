package com.hnu.legal_cases.service;

/**
 * 异步摘要配额（Redis + usage_log）
 */
public interface SummaryQuotaService {
    /**
     * 先扣减用户摘要次数，再 Redis 日限与 usage_log；超限抛 ServiceException，message 以 QUOTA_EXCEEDED 开头
     */
    void tryAcquireSummaryQuota(Long userId, String caseId);

    /**
     * @param usageActionType usage_log.action_type，如 SUMMARY_ASYNC、SUMMARY_SYNC
     */
    void tryAcquireSummaryQuota(Long userId, String caseId, String usageActionType);

    /**
     * 摘要生成失败时退回额度与当日 Redis 计数
     */
    void refundSummaryQuota(Long userId, String caseId);
}
