package com.hnu.legal_cases.dto.cases;

import lombok.Data;

/**
 * 案例基本信息
 *
 * @author baixu
 * @date 2025/7/8
 */
@Data
public class CaseBaseInfo {
    /**
     * 案例ID
     */
    private String case_id;
    /**
     * 案例名称
     */
    private String case_name;
    /**
     * 判决日期
     */
    private String judgement_date;
    /**
     * 国家
     */
    private String country;
    /**
     * 摘要
     */
    private String tags;
    /**
     * AI 摘要中提取出的案例关键词（按请求语言返回）。
     */
    private String keywords;
    /**
     * 引用次数
     */
    private Integer citationCount;
    /**
     * 收藏数
     */
    private Integer favoritedCount;
    /**
     * 是否被收藏
     */
    private Boolean isfavored;
    /**
     * 原判决文书URL
     */
    private String original_document_url;
}
