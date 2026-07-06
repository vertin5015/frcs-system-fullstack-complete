package com.hnu.legal_cases.dto.crawler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 单个数据源的列表搜索结果：区分「接口返回 ok 但无数据」与「未连通 / 非 ok」。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerSingleQueryResult {

    private List<CrawlerBaseInfoItem> items;
    /**
     * true：爬虫 HTTP 接口返回 status 为 ok（条目数可为 0）
     */
    private boolean crawlApiOk;

    public static CrawlerSingleQueryResult failure() {
        return new CrawlerSingleQueryResult(Collections.emptyList(), false);
    }

    public static CrawlerSingleQueryResult success(List<CrawlerBaseInfoItem> items) {
        return new CrawlerSingleQueryResult(items != null ? items : Collections.emptyList(), true);
    }
}
