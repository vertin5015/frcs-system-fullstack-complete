package com.hnu.legal_cases.dto.favorite;

import lombok.Data;

/**
 * 收藏案例请求参数
 */
@Data
public class FavoriteCaseReqVO {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 案例ID
     */
    private String caseId;
}
