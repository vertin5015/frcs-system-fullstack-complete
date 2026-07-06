package com.hnu.legal_cases.dto.user;

import lombok.Data;

/**
 * 注册入参
 */
@Data
public class RegisterReqVO {
    /**
     * 用户名
     */
    private String username;
    /**
     * 电子邮箱
     */
    private String email;
    /**
     * 密码
     */
    private String password;
}
