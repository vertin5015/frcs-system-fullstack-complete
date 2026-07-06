package com.hnu.legal_cases.dto.auth;

import lombok.Data;

/**
 * 发送邮箱验证码：LOGIN=用于验证码登录，RESET=用于忘记密码
 */
@Data
public class SendCodeReqVO {
    private String email;
    /**
     * LOGIN 或 RESET
     */
    private String purpose;
}
