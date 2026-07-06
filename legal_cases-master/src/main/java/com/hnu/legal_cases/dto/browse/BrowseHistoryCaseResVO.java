package com.hnu.legal_cases.dto.browse;

import lombok.Data;

import java.util.List;

/**
 * 浏览历史案例返回参数
 */
@Data
public class BrowseHistoryCaseResVO {
    /**
     * 总数
     */
    private Integer totalCount;
    /**
     * 浏览历史案例列表
     */
    private List<BrowseHistoryInfo> browseHistoryInfoList;
}
