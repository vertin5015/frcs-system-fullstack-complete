package com.hnu.legal_cases.dto.auth;

import lombok.Data;

@Data
public class LoginByCodeReqVO {
    private String email;
    private String code;
}
