package com.hnu.legal_cases.dto.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 爬虫请求参数
 */
@Data
public class CrawlerDetailArgs {
    @JsonProperty("details_url")
    private String detailUrl;
}