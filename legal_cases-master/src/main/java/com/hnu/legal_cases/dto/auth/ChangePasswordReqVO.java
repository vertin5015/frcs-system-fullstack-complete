package com.hnu.legal_cases.dto.auth;

import lombok.Data;

/**
 * 修改密码（已知旧密码），无需验证码
 */
@Data
public class ChangePasswordReqVO {
    private String email;
    private String oldPassword;
    private String newPassword;
}
