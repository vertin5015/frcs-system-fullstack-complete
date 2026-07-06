package com.hnu.legal_cases.service;

public interface CaseDetailService {
    /**
     * 保存案例详细信息
     *
     * @param caseId    案例Id
     * @param summaryEN 英文总结
     * @param summaryZH 中文总结
     */
    void insertCaseDetail(String caseId, String summaryEN, String summaryZH);

    /**
     * 根据caseId和语言获取案例详细信息
     *
     * @param caseId   案例Id
     * @param language 语言
     * @return 案例详细信息
     */
    String getContent(String caseId, String language);
}
