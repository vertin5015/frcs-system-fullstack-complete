package com.hnu.legal_cases.dto.admin;

import lombok.Data;

@Data
public class AdminGrantCreditsReqVO {
    private Long userId;
    private Integer credits;
    private String remark;
}
