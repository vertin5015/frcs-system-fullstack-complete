package com.hnu.legal_cases.pojo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class CaseInfo {
    /**
     * 主键
     */
    private Long pk;
    /**
     * 案例ID
     */
    private String caseId;
    /**
     * 数据源ID
     */
    private Integer sourceId;
    /**
     * 案例名称
     */
    private String caseName;
    /**
     * 判决日期
     */
    private String judgmentDate;
    /**
     * 引用次数
     */
    private Integer citationCount;
    /**
     * 案例摘要
     */
    private String summary;
    /**
     * 原始文档URL
     */
    private String originalDocumentUrl;
    /**
     * 插入时间
     */
    private Timestamp insertTime;
    /**
     * 更新时间
     */
    private Timestamp updateTime;
}
