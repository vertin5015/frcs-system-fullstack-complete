package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.browse.BrowseHistoryCaseReqVO;
import com.hnu.legal_cases.dto.browse.BrowseHistoryCaseResVO;

public interface BrowseHistoryService {

    /**
     * 记录或更新浏览历史（如果存在则更新，不存在则插入）
     *
     * @param userId 用户Id
     * @param caseId 案例Id
     */
    void saveBrowseHistory(Long userId, String caseId);

    /**
     * 获取用户浏览历史列表
     *
     * @param reqVO 请求参数
     * @return 浏览历史列表
     */
    BrowseHistoryCaseResVO browseHistoryList(BrowseHistoryCaseReqVO reqVO);
}