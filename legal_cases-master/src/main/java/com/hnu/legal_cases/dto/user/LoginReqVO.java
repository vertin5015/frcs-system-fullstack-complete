package com.hnu.legal_cases.dto.user;

import lombok.Data;

/**
 * 登录入参
 */
@Data
public class LoginReqVO {
    /**
     * 电子邮箱
     */
    private String email;
    /**
     * 密码
     */
    private String password;
}
