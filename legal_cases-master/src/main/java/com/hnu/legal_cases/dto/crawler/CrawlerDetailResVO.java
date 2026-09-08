package com.hnu.legal_cases.dto.crawler;

import lombok.Data;

import java.util.List;

/**
 * 爬虫案例详细信息响应数据结构
 */
@Data
public class CrawlerDetailResVO {
    /**
     * 响应状态
     */
    private String status;
    /**
     * 案例详细信息
     */
    private List<String> items;
    /**
     * 失败原因（当 status 非 ok 时返回给上游，便于前端直接展示具体问题）。
     */
    private String message;
}
