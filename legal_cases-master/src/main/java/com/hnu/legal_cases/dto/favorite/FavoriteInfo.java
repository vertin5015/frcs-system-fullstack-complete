package com.hnu.legal_cases.dto.favorite;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.Timestamp;

@Data
public class FavoriteInfo {
    /**
     * 案例ID
     */
    private String caseId;
    /**
     * 案例名称
     */
    private String caseName;
    /**
     * 判决日期
     */
    private String judgementDate;
    /**
     * 国家
     */
    private String country;
    /**
     * 摘要
     */
    private String tags;
    /**
     * 引用次数
     */
    private Integer citationCount;
    /**
     * 原判决文书URL
     */
    private String originalDocumentUrl;
    /**
     * 收藏时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Timestamp favoriteDate;
}
