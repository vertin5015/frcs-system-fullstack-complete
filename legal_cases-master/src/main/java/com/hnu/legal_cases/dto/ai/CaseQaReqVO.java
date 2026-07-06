package com.hnu.legal_cases.dto.ai;

import lombok.Data;

@Data
public class CaseQaReqVO {
    private String caseId;
    private String question;
    private String language;
    private Long userId;
}
