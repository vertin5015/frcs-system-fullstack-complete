package com.hnu.legal_cases.dto.ai;

import lombok.Data;

/**
 * ai响应数据结构
 */
@Data
public class SpringAIResVO {
    /**
     * 响应状态
     */
    private String status;
    /**
     * 响应结果
     */
    private String result;
}

