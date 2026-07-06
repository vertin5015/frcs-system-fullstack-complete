package com.hnu.legal_cases.pojo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class UserFavorites {
    /**
     * 收藏记录ID
     */
    private Long favoriteId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 案例ID
     */
    private String caseId;
    /**
     * 用户自定义案例名称
     */
    @Deprecated
    private String customName;
    /**
     * 用户添加的标签
     */
    @Deprecated
    private String tags;
    /**
     * 收藏时间
     */
    private Timestamp favoriteDate;
    /**
     * 插入时间
     */
    private Timestamp insertTime;
    /**
     * 更新时间
     */
    private Timestamp updateTime;
}
