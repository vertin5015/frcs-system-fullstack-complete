package com.hnu.legal_cases.dto.agent;

import lombok.Data;

@Data
public class AgentAskReqVO {
    private Long userId;
    private String question;
    private String language = "zh";
    private String country;
    private String sources;
    private Integer period;
    private Integer topK = 5;
    private Boolean refreshCases = false;
}
