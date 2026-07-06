package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dao.BrowseHistoryMapper;
import com.hnu.legal_cases.dao.CaseMapper;
import com.hnu.legal_cases.dto.browse.BrowseHistoryCaseReqVO;
import com.hnu.legal_cases.dto.browse.BrowseHistoryCaseResVO;
import com.hnu.legal_cases.dto.browse.BrowseHistoryInfo;
import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.enums.PeriodEnum;
import com.hnu.legal_cases.pojo.BrowseHistory;
import com.hnu.legal_cases.pojo.CaseInfo;
import com.hnu.legal_cases.service.BrowseHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;
    @Autowired
    private CaseMapper caseMapper;

    /**
     * 记录或更新浏览历史（如果存在则更新，不存在则插入）
     *
     * @param userId 用户Id
     * @param caseId 案例Id
     */
    @Override
    public void saveBrowseHistory(Long userId, String caseId) {
        BrowseHistory existingHistory = browseHistoryMapper.existsByUserIdAndCaseId(userId, caseId);
        if (existingHistory != null) {
            // 如果存在，更新浏览时间
            browseHistoryMapper.update(existingHistory.getHistoryId());
        } else {
            browseHistoryMapper.insert(userId, caseId);
        }
        log.info("保存案例浏览历史成功 userId：{} , caseId：{}", userId, caseId);
    }

    /**
     * 获取用户浏览历史
     *
     * @param reqVO 请求参数
     * @return 浏览历史列表
     */
    @Override
    public BrowseHistoryCaseResVO browseHistoryList(BrowseHistoryCaseReqVO reqVO) {
        Long userId = reqVO.getUserId();
        String language = reqVO.getLanguage();
        String startDate = PeriodEnum.getStartDateByYear(reqVO.getPeriod());
        Integer sourceId = CountryEnum.getSourceIdByCode(reqVO.getCountry());
        int startIndex = reqVO.getStartIndex();
        int endIndex = reqVO.getEndIndex();

        BrowseHistoryCaseResVO resVO = new BrowseHistoryCaseResVO();

        // 查询该用户所有的浏览历史
        List<BrowseHistory> browseHistoryList = browseHistoryMapper.queryBrowseHistory(userId);
        if (browseHistoryList == null || browseHistoryList.isEmpty()) {
            resVO.setTotalCount(0);
            resVO.setBrowseHistoryInfoList(new ArrayList<>());
            return resVO;
        }

        // 筛选出所有符合条件的浏览历史
        List<BrowseHistoryInfo> browseHistoryInfoList = this.filterBrowseHistory(browseHistoryList, sourceId, startDate, language);

        // 组装结果
        resVO.setTotalCount(browseHistoryInfoList.size());
        if (browseHistoryInfoList.size() <= startIndex) {
            resVO.setBrowseHistoryInfoList(new ArrayList<>());
            return resVO;
        }
        resVO.setBrowseHistoryInfoList(browseHistoryInfoList.subList(startIndex, Math.min(endIndex, browseHistoryInfoList.size())));
        return resVO;
    }

    /**
     * 筛选出所有符合条件的浏览历史
     * 条件：国家、判决时间
     */
    private List<BrowseHistoryInfo> filterBrowseHistory(List<BrowseHistory> browseHistoryList, Integer sourceId, String startDate, String language) {
        Set<String> caseIdList = browseHistoryList.stream().map(BrowseHistory::getCaseId).collect(Collectors.toSet());

        // 获取筛选后结果
        List<CaseInfo> caseInfoList = caseMapper.queryCasesBySourceIdAndDate(caseIdList, language, sourceId, startDate);
        if (caseInfoList == null || caseInfoList.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, CaseInfo> caseInfoMap = caseInfoList.stream()
                .collect(Collectors.toMap(CaseInfo::getCaseId, Function.identity()));

        // 组装BrowseHistoryInfo列表
        List<BrowseHistoryInfo> browseHistoryInfoList = new ArrayList<>();
        for (BrowseHistory browseHistory : browseHistoryList) {
            String caseId = browseHistory.getCaseId();
            CaseInfo caseInfo = caseInfoMap.get(caseId);

            // 只有当案例信息存在时才添加到结果列表
            if (caseInfo != null) {
                BrowseHistoryInfo browseHistoryInfo = new BrowseHistoryInfo();
                browseHistoryInfo.setCaseId(caseId);
                browseHistoryInfo.setCaseName(caseInfo.getCaseName());
                browseHistoryInfo.setJudgementDate(caseInfo.getJudgmentDate());
                browseHistoryInfo.setCountry(CountryEnum.getCodeBySourceId(caseInfo.getSourceId()));
                browseHistoryInfo.setCitationCount(caseInfo.getCitationCount());
                browseHistoryInfo.setTags(caseInfo.getSummary());
                browseHistoryInfo.setOriginalDocumentUrl(caseInfo.getOriginalDocumentUrl());
                browseHistoryInfo.setBrowseTime(browseHistory.getBrowseTime());

                browseHistoryInfoList.add(browseHistoryInfo);
            }
        }

        // 按浏览时间排序
        browseHistoryInfoList.sort((f1, f2) -> f2.getBrowseTime().compareTo(f1.getBrowseTime()));
        return browseHistoryInfoList;
    }
}