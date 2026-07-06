package com.hnu.legal_cases.dto.auth;

import lombok.Data;

/**
 * 发送成功后的前端展示：开发环境可带 devCode 便于无邮件时调试
 */
@Data
public class SendCodeResVO {
    private int cooldownSeconds;
    /**
     * 仅当 app.auth.expose-code-in-response=true 时返回，生产务必关闭
     */
    private String devCode;
}
