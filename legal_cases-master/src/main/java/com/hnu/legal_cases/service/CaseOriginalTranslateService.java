package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.ai.CaseOriginalTranslationVO;

/**
 * 案例原始判决文书翻译服务：将爬虫抓取的原文按段落切分后交给大模型翻译。
 */
public interface CaseOriginalTranslateService {

    /**
     * 翻译某个案例的原始判决文书正文。
     *
     * @param caseId         案例 ID
     * @param targetLanguage 目标语言 zh / en
     * @return 分段后的翻译结果
     */
    CaseOriginalTranslationVO translateOriginal(String caseId, String targetLanguage);
}
