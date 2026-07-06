package com.hnu.legal_cases.dto.kb;

import lombok.Data;

@Data
public class KbIngestDbReqVO {
    /**
     * 入库数量上限，默认 100，最大 2000。
     */
    private Integer limit;

    /**
     * 优先语言：zh / en，默认 zh。
     */
    private String language;
}
