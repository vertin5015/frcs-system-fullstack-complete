package com.hnu.legal_cases.dto.cases;

import lombok.Data;

/**
 * 列表流式推送时的预览项（来自爬虫、尚未经 DB 收藏态），字段尽量与 {@link CaseBaseInfo} 对齐便于前端复用卡片。
 */
@Data
public class SearchPreviewCaseDTO {
    private String case_id;
    private String case_name;
    private String judgement_date;
    private String country;
    private String tags;
    /**
     * AI 摘要中提取出的案例关键词（按请求语言返回）。
     */
    private String keywords;
    private Integer citationCount;
    private Boolean isfavored;
    private Integer favoritedCount;
    private String original_document_url;
}
