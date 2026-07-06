package com.hnu.legal_cases.dto.ai;

import com.hnu.legal_cases.enums.LanguageEnum;
import lombok.Data;

/**
 * ai总结案例入参VO
 *
 * @author baixu
 * @date 2025/9/2
 */
@Data
public class SummaryCaseReqVO {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 案例id
     */
    private String caseId;
    /**
     * 需要返回的语言
     * @see LanguageEnum
     */
    private String language;
}
