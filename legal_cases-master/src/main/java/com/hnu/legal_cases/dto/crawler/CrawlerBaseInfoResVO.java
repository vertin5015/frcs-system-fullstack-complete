package com.hnu.legal_cases.dto.crawler;

import lombok.Data;

import java.util.List;

/**
 * 爬虫案例基本信息响应数据结构
 */
@Data
public class CrawlerBaseInfoResVO {
    /**
     * 响应状态
     */
    private String status;
    /**
     * 响应数据
     */
    private List<CrawlerBaseInfoItem> items;
}

