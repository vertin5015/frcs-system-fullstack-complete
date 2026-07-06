package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.dto.crawler.CrawlerSingleQueryResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * 获取案例基本信息并进行持久化
 *
 * @author baixu
 * @date 2025/8/24
 */
public interface CrawlerService {
    /**
     * 获取案例基本信息（支持多数据源并行查询）
     *
     * @param keyword 搜索关键词
     * @param country 国家代码（可为空，空则查询所有数据源）
     * @param period  时间段（可为空，空则不限制时间）
     * @return 案例基本信息列表
     */
    List<CrawlerBaseInfoItem> queryCaseBaseInfo(String keyword, String country, Integer period);

    /**
     * @param sourcesCsv 逗号分隔国家代码；非空时仅查询这些数据源
     */
    List<CrawlerBaseInfoItem> queryCaseBaseInfo(String keyword, String country, Integer period, String sourcesCsv);

    /**
     * 启动多数据源并行搜索；每路完成时回调（完成顺序即回调顺序，用于流式呈现）。
     *
     * @return 与并行任务对应的 Future 列表，用于后续 {@link #mergeDistinctResults} 与全局等待
     */
    List<CompletableFuture<CrawlerSingleQueryResult>> startParallelCaseSearch(
            String keyword,
            String country,
            Integer period,
            String sourcesCsv,
            BiConsumer<String, CrawlerSingleQueryResult> onSourceDone);

    /**
     * 在 {@link #startParallelCaseSearch} 之后调用：等待（受全局超时约束）并合并、去重；空结果时抛出与同步搜索一致的 {@link com.hnu.legal_cases.exception.ServiceException}。
     */
    List<CrawlerBaseInfoItem> mergeDistinctAfterWait(List<CompletableFuture<CrawlerSingleQueryResult>> futures);

    /**
     * 查询案例详细信息
     *
     * @param url 案例url
     * @return 案例详细信息
     */
    String getCaseDetail(String url);
}
