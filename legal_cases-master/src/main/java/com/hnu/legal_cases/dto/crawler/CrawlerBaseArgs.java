package com.hnu.legal_cases.dto.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 爬虫请求参数
 */
@Data
public class CrawlerBaseArgs {
    @JsonProperty("search_query")
    private String keyword;

    @JsonProperty("year")
    private String year;
}