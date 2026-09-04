package com.hnu.legal_cases.dto.crawler;

import com.hnu.legal_cases.dto.cases.SearchSourceStat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated list-search result, including per-source status for UI display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrawlerSearchResult {

    private List<CrawlerBaseInfoItem> items;

    private List<SearchSourceStat> sourceStats;
}
