package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.cases.CaseBaseInfo;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;

import java.util.List;
import java.util.Set;

public interface CaseService {
    /**
     * 分批保存案例数据到数据库（去重）
     *
     * @param items   爬取的案例数据
     * @param country 国家
     */
    void saveCases(List<CrawlerBaseInfoItem> items, String country);

    /**
     * 根据caseIds和语言获取案例详细信息
     *
     * @param caseIds  案例ID列表
     * @param language 语言
     * @param userId   用户ID
     * @return 案例详细信息列表
     */
    List<CaseBaseInfo> getCasesByLanguage(Set<String> caseIds, String language, Long userId);
}
