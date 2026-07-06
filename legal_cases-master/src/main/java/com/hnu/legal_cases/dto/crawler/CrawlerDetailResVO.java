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
}

