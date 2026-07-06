package com.hnu.legal_cases.pojo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class UsageLog {
    private Long id;
    private Long userId;
    private String caseId;
    private String actionType;
    private Timestamp createdAt;
}
