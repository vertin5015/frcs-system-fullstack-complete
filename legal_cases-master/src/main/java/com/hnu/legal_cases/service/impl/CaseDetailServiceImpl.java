package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dao.CaseDetailMapper;
import com.hnu.legal_cases.enums.LanguageEnum;
import com.hnu.legal_cases.pojo.CaseDetailInfo;
import com.hnu.legal_cases.service.CaseDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CaseDetailServiceImpl implements CaseDetailService {
    @Autowired
    CaseDetailMapper caseDetailMapper;

    /**
     * 保存案例详细信息
     *
     * @param caseId    案例Id
     * @param summaryEN 英文总结
     * @param summaryZH 中文总结
     */
    @Override
    public void insertCaseDetail(String caseId, String summaryEN, String summaryZH) {
        CaseDetailInfo caseDetailInfo = caseDetailMapper.getCaseDetailByCaseId(caseId);
        if (caseDetailInfo != null) {
            log.info("案例详细信息已存在，不再保存，caseId={}", caseId);
            return;
        }

        CaseDetailInfo detailInfo = new CaseDetailInfo();
        detailInfo.setCaseId(caseId);
        detailInfo.setContentEnUs(summaryEN);
        detailInfo.setContentZhCn(summaryZH);
        caseDetailMapper.insert(detailInfo);
    }

    /**
     * 根据caseId和语言获取案例详细信息
     *
     * @param caseId   案例Id
     * @param language 语言
     * @return 案例详细信息
     */
    @Override
    public String getContent(String caseId, String language) {
        CaseDetailInfo caseDetailInfo = caseDetailMapper.getCaseDetailByCaseId(caseId);
        if (caseDetailInfo == null) {
            return null;
        }

        String content = null;
        if (LanguageEnum.ZH_CN.getCode().equals(language)) {
            content = caseDetailInfo.getContentZhCn();
        } else if (LanguageEnum.EN_US.getCode().equals(language)) {
            content = caseDetailInfo.getContentEnUs();
        }

        return content;
    }
}
