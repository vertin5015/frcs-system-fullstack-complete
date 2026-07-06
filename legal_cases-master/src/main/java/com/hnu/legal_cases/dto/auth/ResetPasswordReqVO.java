package com.hnu.legal_cases.dto.auth;

import lombok.Data;

@Data
public class ResetPasswordReqVO {
    private String email;
    private String code;
    private String newPassword;
}
