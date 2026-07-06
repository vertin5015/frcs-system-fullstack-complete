package com.hnu.legal_cases.dto.kb;

import lombok.Data;

@Data
public class KbIngestCrawlerReqVO {
    /**
     * 搜索词（必填）。
     */
    private String keyword;

    /**
     * 国家过滤（可选）：US/EU/JPN。
     */
    private String country;

    /**
     * 时间过滤（可选）：1/3/5/10。
     */
    private Integer period;

    /**
     * 数据源过滤（可选）：逗号分隔 US,EU,JPN。
     */
    private String sources;

    /**
     * 单次最多入库条数，默认 50。
     */
    private Integer limit;
}
