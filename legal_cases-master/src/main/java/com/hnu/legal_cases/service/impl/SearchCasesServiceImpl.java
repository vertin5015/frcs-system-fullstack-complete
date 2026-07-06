package com.hnu.legal_cases.service.impl;

import com.alibaba.fastjson.JSON;
import com.hnu.legal_cases.dao.CaseDetailMapper;
import com.hnu.legal_cases.dao.CaseMapper;
import com.hnu.legal_cases.dto.ai.SummaryCaseReqVO;
import com.hnu.legal_cases.dto.ai.SummaryStatusResVO;
import com.hnu.legal_cases.dto.cases.CaseBaseInfo;
import com.hnu.legal_cases.dto.cases.SearchCasesReqVO;
import com.hnu.legal_cases.dto.cases.SearchCasesResVO;
import com.hnu.legal_cases.dto.cases.SearchPreviewCaseDTO;
import com.hnu.legal_cases.dto.cases.SearchStreamPartDTO;
import com.hnu.legal_cases.dto.crawler.CrawlerBaseInfoItem;
import com.hnu.legal_cases.dto.crawler.CrawlerSingleQueryResult;
import com.hnu.legal_cases.enums.LanguageEnum;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.CaseDetailInfo;
import com.hnu.legal_cases.pojo.CaseInfo;
import com.hnu.legal_cases.service.*;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class SearchCasesServiceImpl implements SearchCasesService {
    private static final long SUMMARY_RUNNING_STALE_MS = 180 * 1000L;
    @Autowired
    private SpringAIService springAIService;
    @Autowired
    private CrawlerService crawlerService;
    @Autowired
    private CaseCacheService caseCacheService;
    @Autowired
    private CaseService caseService;
    @Autowired
    private CaseDetailService caseDetailService;
    @Autowired
    private CaseMapper caseMapper;
    @Autowired
    private BrowseHistoryService browseHistoryService;
    @Autowired
    private CaseDetailMapper caseDetailMapper;
    @Autowired
    private CaseSummaryAsyncService caseSummaryAsyncService;
    @Autowired
    private SummaryQuotaService summaryQuotaService;
    @Autowired
    private LocalKbService localKbService;
    @Autowired
    private OriginalDocumentCacheService originalDocumentCacheService;
    @Autowired
    @Qualifier("caseTaskExecutor")
    private Executor caseTaskExecutor;
    @Autowired
    @Qualifier("crawlerTaskExecutor")
    private Executor crawlerTaskExecutor;

    @Override
    public void searchCasesStream(SearchCasesReqVO reqVO, SearchStreamNotifier notifier) {
        Long userId = reqVO.getUserId();
        String keyword = reqVO.getKeyword();
        String country = reqVO.getCountry();
        String language = reqVO.getLanguage();
        Integer period = reqVO.getPeriod();

        String cacheKey = caseCacheService.generateCacheKey(keyword, country, period, reqVO.getSources());
        if (caseCacheService.hasCache(cacheKey)) {
            SearchCasesResVO resVO = new SearchCasesResVO();
            Set<String> caseIds = caseCacheService.getCachedCaseIds(cacheKey, reqVO.getStartIndex(), reqVO.getEndIndex());
            log.info("命中缓存(stream),案例ID：{}", JSON.toJSONString(caseIds));
            if (CollectionUtils.isEmpty(caseIds)) {
                resVO.setTotalCount(0);
                resVO.setCases(Collections.emptyList());
                notifier.done(resVO);
                return;
            }
            List<CaseBaseInfo> caseBaseInfoList = caseService.getCasesByLanguage(caseIds, language, userId);
            resVO.setTotalCount(caseCacheService.getCachedCount(cacheKey).intValue());
            resVO.setCases(caseBaseInfoList);
            notifier.done(resVO);
            return;
        }

        final String extractedKeyword;
        try {
            extractedKeyword = springAIService.extractKeyword(keyword, country);
        } catch (ServiceException e) {
            notifier.fail(e.getMessage());
            return;
        }
        log.info("提取关键词（stream）：{}", extractedKeyword);

        List<CompletableFuture<CrawlerSingleQueryResult>> futures = crawlerService.startParallelCaseSearch(
                extractedKeyword, country, period, reqVO.getSources(),
                (source, result) -> {
                    List<SearchPreviewCaseDTO> previews = buildPreviewCases(source, result);
                    if (!previews.isEmpty()) {
                        notifier.part(new SearchStreamPartDTO(source, previews));
                    }
                });

        List<CrawlerBaseInfoItem> items;
        try {
            items = crawlerService.mergeDistinctAfterWait(futures);
        } catch (ServiceException e) {
            notifier.fail(e.getMessage());
            return;
        }

        if (CollectionUtils.isEmpty(items)) {
            SearchCasesResVO empty = new SearchCasesResVO();
            empty.setTotalCount(0);
            empty.setCases(Collections.emptyList());
            notifier.done(empty);
            return;
        }

        caseCacheService.cacheCaseIds(cacheKey, items);
        SearchCasesResVO resVO = new SearchCasesResVO();
        try {
            caseService.saveCases(items, country);
            prewarmKnowledgeBase(items);
            prefetchOriginalDocuments(items);
            Set<String> caseIds = caseCacheService.getCachedCaseIds(cacheKey, 0, 9);
            if (CollectionUtils.isEmpty(caseIds)) {
                resVO.setTotalCount(0);
                resVO.setCases(Collections.emptyList());
                notifier.done(resVO);
                return;
            }
            List<CaseBaseInfo> caseBaseInfoList = caseService.getCasesByLanguage(caseIds, language, userId);
            resVO.setTotalCount(caseCacheService.getCachedCount(cacheKey).intValue());
            resVO.setCases(caseBaseInfoList);
            notifier.done(resVO);
        } catch (Throwable e) {
            caseCacheService.deleteCacheKey(cacheKey);
            notifier.fail(e.getMessage() != null ? e.getMessage() : "保存案例失败");
        }
    }

    private List<SearchPreviewCaseDTO> buildPreviewCases(String sourceCode, CrawlerSingleQueryResult result) {
        if (result == null || result.getItems() == null) {
            return List.of();
        }
        List<SearchPreviewCaseDTO> list = new ArrayList<>();
        for (CrawlerBaseInfoItem item : result.getItems()) {
            if (!StringUtils.isNotBlank(item.getTitle())) {
                continue;
            }
            SearchPreviewCaseDTO dto = new SearchPreviewCaseDTO();
            dto.setCase_id(item.getDocketNumber());
            dto.setCase_name(item.getTitle());
            dto.setCountry(sourceCode);
            String sum = item.getSummary();
            if (StringUtils.isNotBlank(sum) && sum.length() > 160) {
                sum = sum.substring(0, 160) + "…";
            }
            dto.setTags(sum);
            dto.setJudgement_date(item.getDateFiled());
            dto.setOriginal_document_url(item.getUrl());
            dto.setIsfavored(false);
            dto.setFavoritedCount(0);
            dto.setCitationCount(parseCitationInt(item.getCitationCount()));
            list.add(dto);
        }
        return list;
    }

    private static Integer parseCitationInt(String raw) {
        if (StringUtils.isBlank(raw)) {
            return 0;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits.length() > 9 ? digits.substring(0, 9) : digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public SearchCasesResVO searchCases(SearchCasesReqVO reqVO) {
        Long userId = reqVO.getUserId();
        String keyword = reqVO.getKeyword();
        String country = reqVO.getCountry();
        String language = reqVO.getLanguage();
        Integer period = reqVO.getPeriod();

        SearchCasesResVO resVO = new SearchCasesResVO();

        String cacheKey = caseCacheService.generateCacheKey(keyword, country, period, reqVO.getSources());
        boolean hasCache = caseCacheService.hasCache(cacheKey);
        if (hasCache) {
            Set<String> caseIds = caseCacheService.getCachedCaseIds(cacheKey, reqVO.getStartIndex(), reqVO.getEndIndex());
            log.info("命中缓存,案例ID：{}", JSON.toJSONString(caseIds));
            if (CollectionUtils.isEmpty(caseIds)) {
                resVO.setTotalCount(0);
                resVO.setCases(Collections.emptyList());
                return resVO;
            }
            List<CaseBaseInfo> caseBaseInfoList = caseService.getCasesByLanguage(caseIds, language, userId);
            resVO.setTotalCount(caseCacheService.getCachedCount(cacheKey).intValue());
            resVO.setCases(caseBaseInfoList);
            return resVO;
        }

        String extractedKeyword = springAIService.extractKeyword(keyword, country);
        log.info("提取关键词：{}", extractedKeyword);

        List<CrawlerBaseInfoItem> items = crawlerService.queryCaseBaseInfo(extractedKeyword, country, period, reqVO.getSources());
        if (CollectionUtils.isEmpty(items)) {
            resVO.setTotalCount(0);
            resVO.setCases(Collections.emptyList());
            return resVO;
        }

        caseCacheService.cacheCaseIds(cacheKey, items);

        try {
            caseService.saveCases(items, country);
            prewarmKnowledgeBase(items);
            prefetchOriginalDocuments(items);
            Set<String> caseIds = caseCacheService.getCachedCaseIds(cacheKey, 0, 9);
            if (CollectionUtils.isEmpty(caseIds)) {
                resVO.setTotalCount(0);
                resVO.setCases(Collections.emptyList());
                return resVO;
            }
            List<CaseBaseInfo> caseBaseInfoList = caseService.getCasesByLanguage(caseIds, language, userId);
            resVO.setTotalCount(caseCacheService.getCachedCount(cacheKey).intValue());
            resVO.setCases(caseBaseInfoList);
            return resVO;
        } catch (Throwable e) {
            caseCacheService.deleteCacheKey(cacheKey);
            throw new ServiceException(e.getMessage());
        }
    }

    private void prewarmKnowledgeBase(List<CrawlerBaseInfoItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        List<CrawlerBaseInfoItem> snapshot = new ArrayList<>(items);
        CompletableFuture.runAsync(() -> {
            try {
                localKbService.ingestCrawlerItems(snapshot, 50);
            } catch (Throwable e) {
                log.warn("搜索结果预热本地知识库失败: {}", e.getMessage());
            }
        }, caseTaskExecutor);
    }

    private void prefetchOriginalDocuments(List<CrawlerBaseInfoItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        List<CrawlerBaseInfoItem> snapshot = new ArrayList<>(items);
        CompletableFuture.runAsync(() -> {
            int count = 0;
            for (CrawlerBaseInfoItem item : snapshot) {
                if (item == null || StringUtils.isBlank(item.getUrl())) {
                    continue;
                }
                originalDocumentCacheService.prefetch(item.getUrl(), item.getDocketNumber(), item.getTitle());
                count++;
                if (count >= 3) {
                    break;
                }
            }
            log.info("搜索结果原文后台预抓已提交 count={}", count);
        }, crawlerTaskExecutor);
    }

    @Override
    public String summaryCases(SummaryCaseReqVO reqVO) {
        Long userId = reqVO.getUserId();
        String caseId = reqVO.getCaseId();
        String language = reqVO.getLanguage();

        String content = caseDetailService.getContent(caseId, language);
        if (StringUtils.isNotBlank(content) && userId != 0L) {
            browseHistoryService.saveBrowseHistory(userId, caseId);
            return content;
        }

        List<CaseInfo> caseBaseInfo = caseMapper.queryCases(Set.of(caseId), language);
        String url = caseBaseInfo.getFirst().getOriginalDocumentUrl();
        if (StringUtils.isBlank(url)) {
            throw new ServiceException("caseId：" + caseId + " 对应的案例url为空");
        }

        if (userId != null && userId != 0L) {
            summaryQuotaService.tryAcquireSummaryQuota(userId, caseId, "SUMMARY_SYNC");
        }
        try {
            String caseDetail = crawlerService.getCaseDetail(url);
            String summaryEN = springAIService.summaryCase(caseDetail);
            String summaryZH = springAIService.translate(summaryEN, "English", "Simplified Chinese");
            summaryZH = this.formatText(summaryZH);
            caseDetailService.insertCaseDetail(caseId, summaryEN, summaryZH);
        } catch (Throwable e) {
            if (userId != null && userId != 0L) {
                summaryQuotaService.refundSummaryQuota(userId, caseId);
            }
            if (e instanceof ServiceException) {
                throw (ServiceException) e;
            }
            throw new ServiceException(e.getMessage() != null ? e.getMessage() : "摘要生成失败");
        }

        if (userId != null && userId != 0L) {
            browseHistoryService.saveBrowseHistory(userId, caseId);
        }

        content = caseDetailService.getContent(caseId, language);
        return content;
    }

    @Override
    public SummaryStatusResVO startAsyncSummary(SummaryCaseReqVO reqVO) {
        return startAsyncSummary(reqVO, false);
    }

    @Override
    public SummaryStatusResVO startAsyncSummary(SummaryCaseReqVO reqVO, boolean forceRegenerate) {
        Long userId = reqVO.getUserId();
        String caseId = reqVO.getCaseId();
        String language = reqVO.getLanguage();

        if (forceRegenerate) {
            caseDetailMapper.resetSummary(caseId);
        }
        String content = caseDetailService.getContent(caseId, language);
        if (!forceRegenerate && StringUtils.isNotBlank(content)) {
            SummaryStatusResVO vo = new SummaryStatusResVO();
            vo.setStatus("DONE");
            vo.setContent(content);
            vo.setSummaryUpdatedAtMs(resolveUpdatedAtMs(caseId));
            if (userId != null && userId != 0L) {
                browseHistoryService.saveBrowseHistory(userId, caseId);
            }
            return vo;
        }

        CaseDetailInfo row = caseDetailMapper.getCaseDetailByCaseId(caseId);
        if (!forceRegenerate && row != null && "RUNNING".equals(row.getSummaryStatus())) {
            if (isRunningStale(row)) {
                caseDetailMapper.updateSummaryStatus(caseId, "FAILED", "摘要任务超时，已自动重置，请重新生成");
                row = caseDetailMapper.getCaseDetailByCaseId(caseId);
            } else {
                return buildSummaryStatusVo(row, language);
            }
        }

        summaryQuotaService.tryAcquireSummaryQuota(userId, caseId);

        try {
            if (row == null) {
                try {
                    caseDetailMapper.insertPlaceholder(caseId);
                } catch (Exception e) {
                    log.warn("insertPlaceholder 并发可能重复 caseId={} msg={}", caseId, e.getMessage());
                }
            } else if ("FAILED".equals(row.getSummaryStatus()) || "PENDING".equals(row.getSummaryStatus())
                    || row.getSummaryStatus() == null || forceRegenerate) {
                caseDetailMapper.updateSummaryStatus(caseId, "RUNNING", null);
            }

            caseSummaryAsyncService.generateSummaryAsync(caseId, userId, language);
        } catch (RuntimeException e) {
            summaryQuotaService.refundSummaryQuota(userId, caseId);
            throw e;
        }

        SummaryStatusResVO vo = new SummaryStatusResVO();
        vo.setStatus("RUNNING");
        vo.setSummaryUpdatedAtMs(resolveUpdatedAtMs(caseId));
        return vo;
    }

    @Override
    public SummaryStatusResVO getSummaryStatus(String caseId, String language) {
        CaseDetailInfo row = caseDetailMapper.getCaseDetailByCaseId(caseId);
        if (row == null) {
            SummaryStatusResVO vo = new SummaryStatusResVO();
            vo.setStatus("IDLE");
            return vo;
        }
        return buildSummaryStatusVo(row, language);
    }

    private SummaryStatusResVO buildSummaryStatusVo(CaseDetailInfo row, String language) {
        SummaryStatusResVO vo = new SummaryStatusResVO();
        vo.setSummaryUpdatedAtMs(toMs(row.getSummaryUpdatedAt()));

        String content = null;
        if (LanguageEnum.ZH_CN.getCode().equals(language)) {
            content = row.getContentZhCn();
        } else if (LanguageEnum.EN_US.getCode().equals(language)) {
            content = row.getContentEnUs();
        }
        if (StringUtils.isNotBlank(content)) {
            vo.setStatus("DONE");
            vo.setContent(content);
            return vo;
        }

        String st = row.getSummaryStatus();
        if ("FAILED".equals(st)) {
            vo.setStatus("FAILED");
            vo.setErrorMessage(row.getSummaryError());
            return vo;
        }
        if ("RUNNING".equals(st) || "PENDING".equals(st)) {
            if ("RUNNING".equals(st) && isRunningStale(row)) {
                try {
                    finalizeStaleRunningSummary(row.getCaseId(), language);
                    CaseDetailInfo refreshed = caseDetailMapper.getCaseDetailByCaseId(row.getCaseId());
                    if (refreshed != null) {
                        return buildSummaryStatusVo(refreshed, language);
                    }
                } catch (Exception e) {
                    log.warn("运行中摘要超时兜底失败 caseId={} msg={}", row.getCaseId(), e.getMessage());
                    vo.setStatus("FAILED");
                    vo.setErrorMessage("摘要任务超时，已自动重置，请重新生成");
                    return vo;
                }
            }
            vo.setStatus("RUNNING");
            return vo;
        }
        vo.setStatus("IDLE");
        return vo;
    }

    private boolean isRunningStale(CaseDetailInfo row) {
        if (row == null || row.getSummaryUpdatedAt() == null) {
            return false;
        }
        return System.currentTimeMillis() - row.getSummaryUpdatedAt().getTime() > SUMMARY_RUNNING_STALE_MS;
    }

    private void finalizeStaleRunningSummary(String caseId, String language) {
        List<CaseInfo> list = caseMapper.queryCases(Set.of(caseId), language);
        String caseName = (list == null || list.isEmpty() || StringUtils.isBlank(list.getFirst().getCaseName()))
                ? caseId
                : list.getFirst().getCaseName();
        String sourceUrl = (list == null || list.isEmpty()) ? "" : list.getFirst().getOriginalDocumentUrl();
        String en = "## Case Summary (Emergency Fallback)\n\n"
                + "The async summary task timed out, so a local fallback summary is provided.\n\n"
                + "- Case: " + caseName + "\n"
                + "- Source: " + (StringUtils.isBlank(sourceUrl) ? "N/A" : sourceUrl) + "\n";
        String zh = "## 案件摘要（超时兜底）\n\n"
                + "异步摘要任务超时，已自动提供本地兜底摘要。\n\n"
                + "- 案件：" + caseName + "\n"
                + "- 来源：" + (StringUtils.isBlank(sourceUrl) ? "无" : sourceUrl) + "\n";
        caseDetailMapper.updateSummaryDone(caseId, zh, en);
    }

    private Long resolveUpdatedAtMs(String caseId) {
        CaseDetailInfo row = caseDetailMapper.getCaseDetailByCaseId(caseId);
        return row == null ? null : toMs(row.getSummaryUpdatedAt());
    }

    private Long toMs(java.sql.Timestamp ts) {
        return ts == null ? null : ts.getTime();
    }

    public String formatText(String text) {
        if (text == null) {
            return null;
        }
        String result = text;
        result = result.replaceAll("(#+)(?![# ])", "$1 ");
        result = result.replaceAll(" +#", "#");
        result = result.replaceAll("\n", "\n\n");
        result = result.replaceAll("-(?! )", "- ");
        result = result.replaceAll("(\\d)。(?! )", "$1. ");
        result = result.replaceAll("(\\d)。(?= )", "$1.");
        result = result.replaceAll("(\\d)\\.(?! )", "$1. ");
        return result;
    }
}
