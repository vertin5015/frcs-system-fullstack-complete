package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dao.CaseMapper;
import com.hnu.legal_cases.dto.cases.CaseBaseInfo;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.CaseInfo;
import com.hnu.legal_cases.service.CaseService;
import com.hnu.legal_cases.service.FavoriteService;
import com.hnu.legal_cases.service.SpringAIService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CaseServiceImpl implements CaseService {
    @Autowired
    CaseMapper caseMapper;
    @Autowired
    private FavoriteService favoriteService;
    @Autowired
    private SpringAIService springAIService;

    /**
     * 分批保存案例数据到数据库（去重）
     *
     * @param items   爬取的案例数据
     * @param country 国家枚举
     */
    @Override
    public void saveCases(List<CrawlerBaseInfoItem> items, String country) {
        if (CollectionUtils.isEmpty(items)) {
            log.info("案例数据为空，无需保存");
            return;
        }

        // 1. 提取所有caseId并去重
        List<String> allCaseIds = items.stream()
                .map(CrawlerBaseInfoItem::getDocketNumber)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(allCaseIds)) {
            throw new ServiceException("所有案例的Id都为空，无法保存");
        }

        // 2. 查询已存在的案例ID
        Set<String> existingCaseIds = new HashSet<>(caseMapper.queryExistingCaseIds(allCaseIds));
        log.info("数据库中已存在的案例数量：{}", existingCaseIds.size());

        // 3. 过滤出不存在的案例（按 trim 后的案号与库中比对，避免空格导致误判）
        List<CrawlerBaseInfoItem> newItems = items.stream()
                .filter(item -> StringUtils.isNotBlank(item.getDocketNumber())
                        && !existingCaseIds.contains(item.getDocketNumber().trim()))
                .collect(Collectors.toList());
        // 同一批爬取结果里可能重复出现同一案号；并行保存时也会并发插入同一案号，必须先按案号去重
        newItems = newItems.stream()
                .filter(item -> StringUtils.isNotBlank(item.getDocketNumber()))
                .collect(Collectors.toMap(
                        item -> item.getDocketNumber().trim(),
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new))
                .values()
                .stream()
                .collect(Collectors.toList());
        log.info("需要保存的新案例数量：{}", newItems.size());
        if (CollectionUtils.isEmpty(newItems)) {
            return;
        }

        // 搜索链路必须快速返回：这里只做快速落库，不再逐条同步调用 AI 翻译/抽词。
        for (CrawlerBaseInfoItem item : newItems) {
            this.processSingleCase(item);
        }

        log.info("案例数据保存完成");
    }

    /**
     * 根据caseIds和语言获取案例基本信息
     *
     * @param caseIds  案例ID列表
     * @param language 语言
     * @param userId   用户Id
     * @return 案例详细基本列表
     */
    @Override
    public List<CaseBaseInfo> getCasesByLanguage(Set<String> caseIds, String language, Long userId) {
        if (CollectionUtils.isEmpty(caseIds)) {
            throw new ServiceException("查询案例基本信息入参caseId为空");
        }

        List<CaseBaseInfo> caseBaseInfoList = new ArrayList<>();

        List<CaseInfo> caseInfoList = caseMapper.queryCases(caseIds, language);
        for (CaseInfo caseInfo : caseInfoList) {
            CaseBaseInfo caseBaseInfo = new CaseBaseInfo();
            caseBaseInfo.setCase_id(caseInfo.getCaseId());
            caseBaseInfo.setCase_name(caseInfo.getCaseName());
            caseBaseInfo.setJudgement_date(caseInfo.getJudgmentDate());
            caseBaseInfo.setCountry(CountryEnum.getCodeBySourceId(caseInfo.getSourceId()));
            caseBaseInfo.setTags(caseInfo.getSummary());
            caseBaseInfo.setCitationCount(caseInfo.getCitationCount());
            caseBaseInfo.setOriginal_document_url(caseInfo.getOriginalDocumentUrl());
            caseBaseInfoList.add(caseBaseInfo);
        }

        for (CaseBaseInfo caseBaseInfo : caseBaseInfoList) {
            caseBaseInfo.setIsfavored(favoriteService.isFavorite(userId, caseBaseInfo.getCase_id()));
            caseBaseInfo.setFavoritedCount(favoriteService.getFavoritedCount(caseBaseInfo.getCase_id()));
        }

        return caseBaseInfoList;
    }

    private void processSingleCase(CrawlerBaseInfoItem item) {
        try {
            // 步骤1：构建英文案例数据
            CaseInfo enCase = this.buildEnCase(item);

            // 步骤2：构建中文案例数据
            CaseInfo zhCase = this.buildZhCase(enCase);

            // 步骤3：保存到数据库
            caseMapper.insertEnCases(enCase);
            caseMapper.insertZhCases(zhCase);
        } catch (Exception e) {
            if (isDuplicateKeyException(e)) {
                log.debug("案号已存在，跳过插入: {}", item.getDocketNumber());
                return;
            }
            throw new ServiceException("处理案例失败：" + item.getDocketNumber() + "，" + e.getMessage());
        }
    }

    private static boolean isDuplicateKeyException(Throwable e) {
        while (e != null) {
            if (e instanceof SQLIntegrityConstraintViolationException) {
                return true;
            }
            String msg = e.getMessage();
            if (msg != null && msg.contains("Duplicate entry")) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    /**
     * 构建英文案例数据
     */
    private CaseInfo buildEnCase(CrawlerBaseInfoItem item) {
        CaseInfo enCase = new CaseInfo();

        // 处理标题（如果是日语需要翻译）
        String title = item.getTitle();
        if (Objects.equals(item.getSourceId(), CountryEnum.JPN.getSourceId()) && StringUtils.isNotBlank(title)) {
            title = springAIService.translate(title, "Japanese", "English");
        }
        enCase.setCaseName(title);

        // 处理摘要：优先使用爬虫返回内容，避免搜索时逐条调用 AI 导致页面长时间等待。
        String text = item.getSummary();
        if (StringUtils.isBlank(text)) {
            text = item.getTitle();
        }
        enCase.setSummary(compactText(text, 180));

        // 处理引用次数（为空时存储为0）
        if (StringUtils.isNotBlank(item.getCitationCount())) {
            try {
                enCase.setCitationCount(Integer.parseInt(item.getCitationCount()));
            } catch (NumberFormatException e) {
                throw new ServiceException("引用次数非数字：" + item.getCitationCount());
            }
        } else {
            enCase.setCitationCount(0);
        }

        enCase.setCaseId(StringUtils.isNotBlank(item.getDocketNumber()) ? item.getDocketNumber().trim() : item.getDocketNumber());
        enCase.setSourceId(item.getSourceId());
        enCase.setJudgmentDate(item.getDateFiled());
        enCase.setOriginalDocumentUrl(item.getUrl());

        return enCase;
    }

    /**
     * 翻译并构建中文案例数据
     */
    private CaseInfo buildZhCase(CaseInfo enCase) {
        CaseInfo zhCase = new CaseInfo();

        // 搜索结果先用原文标题和摘要快速展示，完整中文处理交给后续摘要/知识库链路。
        String caseName = enCase.getCaseName();
        zhCase.setCaseName(caseName);

        String summary = enCase.getSummary();
        zhCase.setSummary(summary);

        zhCase.setCaseId(enCase.getCaseId());
        zhCase.setSourceId(enCase.getSourceId());
        zhCase.setJudgmentDate(enCase.getJudgmentDate());
        zhCase.setCitationCount(enCase.getCitationCount());
        zhCase.setOriginalDocumentUrl(enCase.getOriginalDocumentUrl());

        return zhCase;
    }

    private String compactText(String text, int maxLen) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() > maxLen ? compact.substring(0, maxLen) : compact;
    }
}

