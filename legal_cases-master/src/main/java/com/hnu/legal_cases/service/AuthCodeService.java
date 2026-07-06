package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.auth.SendCodeResVO;

/**
 * 邮箱验证码：Redis 存储；邮件未配置时依赖日志或 dev 回显
 */
public interface AuthCodeService {

    SendCodeResVO sendCode(String email, String purpose);

    void verifyAndDelete(String email, String purpose, String code);
}
