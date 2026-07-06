package com.hnu.legal_cases.pojo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class CaseDetailInfo {
    /**
     * 主键
     */
    private Long pk;
    /**
     * 案例ID
     */
    private String caseId;
    /**
     * 中文详细信息
     */
    private String contentZhCn;
    /**
     * 英文详细信息
     */
    private String contentEnUs;
    /**
     * 摘要任务状态：PENDING / RUNNING / DONE / FAILED
     */
    private String summaryStatus;
    private String summaryError;
    private Timestamp summaryUpdatedAt;
    /**
     * 插入时间
     */
    private Timestamp insertTime;
    /**
     * 更新时间
     */
    private Timestamp updateTime;
}
