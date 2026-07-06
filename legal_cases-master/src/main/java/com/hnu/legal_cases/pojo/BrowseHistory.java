package com.hnu.legal_cases.pojo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class BrowseHistory {
    /**
     * 浏览历史ID
     */
    private Long historyId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 案例ID
     */
    private String caseId;
    /**
     * 浏览时间
     */
    private Timestamp browseTime;
    /**
     * 插入时间
     */
    private Timestamp insertTime;
    /**
     * 更新时间
     */
    private Timestamp updateTime;
}
