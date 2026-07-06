package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dao.CaseDetailMapper;
import com.hnu.legal_cases.dao.CaseMapper;
import com.hnu.legal_cases.pojo.CaseInfo;
import com.hnu.legal_cases.service.BrowseHistoryService;
import com.hnu.legal_cases.service.CaseSummaryAsyncService;
import com.hnu.legal_cases.service.OriginalDocumentCacheService;
import com.hnu.legal_cases.service.SpringAIService;
import com.hnu.legal_cases.service.SummaryQuotaService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CaseSummaryAsyncServiceImpl implements CaseSummaryAsyncService {

    @Autowired
    private SpringAIService springAIService;
    @Autowired
    private OriginalDocumentCacheService originalDocumentCacheService;
    @Autowired
    private CaseMapper caseMapper;
    @Autowired
    private CaseDetailMapper caseDetailMapper;
    @Autowired
    private BrowseHistoryService browseHistoryService;
    @Autowired
    private SummaryQuotaService summaryQuotaService;

    @Override
    @Async("caseTaskExecutor")
    public void generateSummaryAsync(String caseId, Long userId, String language) {
        try {
            List<CaseInfo> caseBaseInfo = caseMapper.queryCases(Set.of(caseId), language);
            if (caseBaseInfo == null || caseBaseInfo.isEmpty()) {
                throw new IllegalStateException("案例不存在: " + caseId);
            }
            String url = caseBaseInfo.getFirst().getOriginalDocumentUrl();
            if (StringUtils.isBlank(url)) {
                throw new IllegalStateException("案例 URL 为空");
            }
            String caseName = caseBaseInfo.getFirst().getCaseName();
            String caseDetail = fetchCaseDetailWithTimeout(url, caseId, caseName);
            if (isBlankCrawlerText(caseDetail)) {
                caseDetail = buildMetadataDocument(caseId, caseName, url);
            }
            caseDetail = limitSummaryInput(caseDetail, caseName, url);

            boolean zh = language == null || language.toLowerCase().startsWith("zh");
            String primarySummary = springAIService.summaryCase(caseDetail, zh ? "zh" : "en");
            String summaryZH;
            String summaryEN;
            if (zh) {
                summaryZH = ensureChineseSummary(primarySummary, caseDetail, caseId, caseName, url);
                summaryEN = "";
            } else {
                summaryEN = primarySummary;
                summaryZH = ensureChineseSummary(summaryEN, caseDetail, caseId, caseName, url);
            }
            summaryZH = formatText(summaryZH);
            caseDetailMapper.updateSummaryDone(caseId, summaryZH, summaryEN);
            if (userId != null && userId != 0L) {
                browseHistoryService.saveBrowseHistory(userId, caseId);
            }
        } catch (Throwable e) {
            log.error("异步生成摘要失败 caseId={}", caseId, e);
            if (userId != null && userId != 0L) {
                summaryQuotaService.refundSummaryQuota(userId, caseId);
            }
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.length() > 2000) {
                msg = msg.substring(0, 2000);
            }
            caseDetailMapper.updateSummaryStatus(caseId, "FAILED", msg);
        }
    }

    private String formatText(String text) {
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

    private String fetchCaseDetailWithTimeout(String url, String caseId, String caseName) {
        String cached = originalDocumentCacheService.getCachedText(url);
        if (StringUtils.isNotBlank(cached)) {
            log.info("AI 摘要复用原文缓存 url={} chars={}", url, cached.length());
            return cached;
        }
        originalDocumentCacheService.prefetch(url, caseId, caseName);
        String fetched = originalDocumentCacheService.fetchOrGet(url, 25, TimeUnit.SECONDS);
        if (StringUtils.isNotBlank(fetched)) {
            return fetched;
        }
        log.warn("抓取案例正文超时或失败，使用简版内容继续摘要 url={}", url);
        return "Case Name: " + (caseName == null ? "" : caseName) + "\nSource URL: " + url;
    }

    private boolean isBlankCrawlerText(String text) {
        if (StringUtils.isBlank(text)) {
            return true;
        }
        String compact = text.replaceAll("\\s+", "");
        return compact.length() < 40;
    }

    private String buildMetadataDocument(String caseId, String caseName, String url) {
        return "Case Name: " + (caseName == null ? "" : caseName) + "\n"
                + "Case ID: " + (caseId == null ? "" : caseId) + "\n"
                + "Source URL: " + (url == null ? "" : url) + "\n\n"
                + "Note: The original source did not return full text yet. Summarize the available case metadata clearly.";
    }

    private String ensureChineseSummary(String summary, String sourceText, String caseId, String caseName, String url) {
        if (StringUtils.isBlank(summary)) {
            return buildChineseStructuredFallback(caseId, caseName, url, sourceText);
        }
        String trimmed = summary.trim();
        int chineseChars = countChineseChars(trimmed);
        if (chineseChars >= 20 && !looksGarbled(trimmed)) {
            return trimmed;
        }
        if (looksGarbled(trimmed)) {
            return buildChineseStructuredFallback(caseId, caseName, url, sourceText);
        }
        String translated = springAIService.translate(trimmed, "English", "Simplified Chinese");
        if (isUsableChinese(translated, trimmed)) {
            return translated.trim();
        }
        return buildChineseSummaryFromEnglish(trimmed, caseId, caseName, url);
    }

    private boolean isUsableChinese(String translated, String original) {
        if (StringUtils.isBlank(translated)) {
            return false;
        }
        String t = translated.trim();
        return countChineseChars(t) >= 20
                && !looksGarbled(t)
                && !t.equals(original == null ? "" : original.trim());
    }

    private int countChineseChars(String text) {
        if (text == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= '\u4e00' && ch <= '\u9fff') {
                count++;
            }
        }
        return count;
    }

    private boolean looksGarbled(String text) {
        if (StringUtils.isBlank(text)) {
            return true;
        }
        long bad = text.chars().filter(ch -> ch == '\uFFFD' || ch == '?').count();
        long mojibake = text.chars().filter(ch -> ch == '�' || ch == 'ð' || ch == 'Ð' || ch == 'Â').count();
        return bad + mojibake > Math.max(10, text.length() / 10);
    }

    private String buildChineseSummaryFromEnglish(String englishSummary, String caseId, String caseName, String url) {
        return "## 案件摘要\n\n"
                + "本地中文模型输出异常，以下先保留 AI 生成的英文摘要，并补充中文案件信息，避免展示乱码。\n\n"
                + "- 案件名称：" + (StringUtils.isBlank(caseName) ? caseId : caseName) + "\n"
                + "- 案件编号：" + (StringUtils.isBlank(caseId) ? "未知" : caseId) + "\n"
                + "- 原文来源：" + (StringUtils.isBlank(url) ? "暂无" : url) + "\n\n"
                + "### AI Summary (English)\n\n"
                + englishSummary;
    }

    private String buildChineseStructuredFallback(String caseId, String caseName, String url, String sourceText) {
        return "## 案件摘要\n\n"
                + "当前原文或本地模型输出不可用，系统基于已获取信息生成中文摘要。\n\n"
                + "- 案件名称：" + (StringUtils.isBlank(caseName) ? caseId : caseName) + "\n"
                + "- 案件编号：" + (StringUtils.isBlank(caseId) ? "未知" : caseId) + "\n"
                + "- 原文来源：" + (StringUtils.isBlank(url) ? "暂无" : url) + "\n\n"
                + "### 可用原文片段\n\n"
                + (StringUtils.isBlank(sourceText) ? "暂无可用正文。" : sourceText);
    }

    private String limitSummaryInput(String caseDetail, String caseName, String url) {
        String safe = caseDetail == null ? "" : caseDetail.trim();
        int maxSummaryChars = 3500;
        if (safe.length() <= maxSummaryChars) {
            return safe;
        }
        return "Case Name: " + (caseName == null ? "" : caseName) + "\n"
                + "Source URL: " + (url == null ? "" : url) + "\n\n"
                + safe.substring(0, maxSummaryChars);
    }
}
