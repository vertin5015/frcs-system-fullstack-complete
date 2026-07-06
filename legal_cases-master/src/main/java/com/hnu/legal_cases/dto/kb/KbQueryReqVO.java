package com.hnu.legal_cases.dto.kb;

import lombok.Data;

@Data
public class KbQueryReqVO {
    /**
     * 用户问题。
     */
    private String question;

    /**
     * 返回语言，默认 zh。
     */
    private String language;

    /**
     * 召回数量，可选。
     */
    private Integer topK;
}
