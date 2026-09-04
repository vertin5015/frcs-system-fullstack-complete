package com.hnu.legal_cases.dto.cases;

import lombok.Data;

import java.util.ArrayList;
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

    /**
     * 各数据源结果统计，用于前端展示“美国 x 条 / 日本 x 条”以及区分 0 条、失败、超时。
     */
    private List<SearchSourceStat> sourceStats = new ArrayList<>();
}
