package com.hnu.legal_cases.dto.crawler;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 爬虫返回的案例项
 */
@Data
public class CrawlerBaseInfoItem {
    @JsonProperty("source_id")
    private Integer sourceId;

    @JsonProperty("Docket Number")
    @JsonAlias({"docket_number", "DocketNumber", "case_id", "Case ID", "caseId"})
    private String docketNumber;

    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("Date Filed")
    private String dateFiled;

    @JsonProperty("citation_count")
    private String citationCount;

    @JsonProperty("summary")
    private String summary;
}