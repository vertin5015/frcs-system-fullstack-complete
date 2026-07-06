package com.hnu.legal_cases.dto.user;

import lombok.Data;

/**
 * 登录出参
 */
@Data
public class LoginResVO {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户名
     */
    private String username;
    /**
     * AI 摘要剩余次数
     */
    private Integer summaryCredits;
}
