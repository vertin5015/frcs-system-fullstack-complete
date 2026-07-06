package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.ai.SummaryCaseReqVO;
import com.hnu.legal_cases.dto.ai.SummaryStatusResVO;
import com.hnu.legal_cases.dto.cases.SearchCasesReqVO;
import com.hnu.legal_cases.dto.cases.SearchCasesResVO;

/**
 * 搜索案例基本信息
 */
public interface SearchCasesService {

    SearchCasesResVO searchCases(SearchCasesReqVO reqVO);

    /**
     * 流式搜索：爬虫每路先返回时通过 notifier 推送，最终 {@link #done} 写入 DB 后的第一页结果。
     */
    void searchCasesStream(SearchCasesReqVO reqVO, SearchStreamNotifier notifier);

    String summaryCases(SummaryCaseReqVO reqVO);

    SummaryStatusResVO startAsyncSummary(SummaryCaseReqVO reqVO);

    SummaryStatusResVO startAsyncSummary(SummaryCaseReqVO reqVO, boolean forceRegenerate);

    SummaryStatusResVO getSummaryStatus(String caseId, String language);
}
