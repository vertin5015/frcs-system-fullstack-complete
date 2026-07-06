package com.hnu.legal_cases.dto.ai;

import lombok.Data;

/**
 * 异步摘要状态与内容
 */
@Data
public class SummaryStatusResVO {
    /**
     * IDLE / RUNNING / DONE / FAILED
     */
    private String status;
    private String content;
    private String errorMessage;
    /**
     * 摘要更新时间（毫秒时间戳，可为空）
     */
    private Long summaryUpdatedAtMs;
}
