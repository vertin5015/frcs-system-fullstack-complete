package com.hnu.legal_cases.dto.cases;

import lombok.Data;

import java.util.List;

/**
 * 搜索案例出参VO
 *
 * @author baixu
 * @date 2025/7/8
 */
@Data
public class SearchCasesResVO {
    /**
     * 案例总数
     */
    private Integer totalCount;
    /**
     * 案例列表
     */
    private List<CaseBaseInfo> cases;
}
