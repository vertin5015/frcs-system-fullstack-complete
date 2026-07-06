package com.hnu.legal_cases.dto.kb;

import lombok.Data;

@Data
public class KbIngestReqVO {
    /**
     * 业务主键（可选），建议传案例 ID。
     */
    private String sourceId;

    /**
     * 标题（可选）。
     */
    private String title;

    /**
     * 原文内容；不传时且 sourceId 为 caseId，则尝试从 case_detail_info 读取。
     */
    private String content;

    /**
     * 可选语言标记，仅用于日志与后续扩展（zh/en）。
     */
    private String language;
}
