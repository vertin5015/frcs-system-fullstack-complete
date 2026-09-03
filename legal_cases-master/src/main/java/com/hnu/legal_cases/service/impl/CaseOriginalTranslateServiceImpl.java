package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dao.CaseMapper;
import com.hnu.legal_cases.dto.ai.CaseOriginalTranslationVO;
import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.pojo.CaseInfo;
import com.hnu.legal_cases.service.CaseOriginalTranslateService;
import com.hnu.legal_cases.service.OriginalDocumentCacheService;
import com.hnu.legal_cases.service.SpringAIService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 原文翻译实现：
 * 1. 从原文正文缓存/爬虫兜底取得判决文书文本；
 * 2. 先按语义断句把整篇正文切成“段落”级别的小块（方便阅读也方便逐段调用模型）；
 * 3. 原文已是目标语言时直接返回分段原文，否则逐段调用 Spring AI 翻译；
 * 4. 结果按 (url + 目标语言) 内存缓存，避免重复消耗模型调用。
 */
@Slf4j
@Service
public class CaseOriginalTranslateServiceImpl implements CaseOriginalTranslateService {

    /** 单段最大字符数：过长段落会先按句号/空格拆开再翻译 */
    static final int MAX_SEGMENT_CHARS = 1600;

    /** 整篇原文参与翻译的最大字符数，防止长判决书导致几十次模型调用 */
    static final int MAX_TOTAL_CHARS = 30000;

    private static final String LANG_ZH = "zh";
    private static final String LANG_EN = "en";

    @Autowired
    private CaseMapper caseMapper;

    @Autowired
    private OriginalDocumentCacheService originalDocumentCacheService;

    @Autowired
    private SpringAIService springAIService;

    private final Map<String, CaseOriginalTranslationVO> translationCache = new ConcurrentHashMap<>();

    @Override
    public CaseOriginalTranslationVO translateOriginal(String caseId, String targetLanguage) {
        if (StringUtils.isBlank(caseId) || StringUtils.isBlank(targetLanguage)) {
            throw new ServiceException("caseId 与 language 不能为空");
        }
        String target = targetLanguage.trim().toLowerCase(Locale.ROOT);
        if (!LANG_ZH.equals(target) && !LANG_EN.equals(target)) {
            throw new ServiceException("language 仅支持 zh / en");
        }

        List<CaseInfo> caseInfos = caseMapper.queryCases(Set.of(caseId.trim()), LANG_EN);
        if (caseInfos == null || caseInfos.isEmpty()) {
            throw new ServiceException("案例不存在: " + caseId);
        }
        CaseInfo caseInfo = caseInfos.get(0);
        String url = caseInfo.getOriginalDocumentUrl();
        if (StringUtils.isBlank(url)) {
            throw new ServiceException("该案例缺少原始文书链接");
        }

        String cacheKey = url.trim() + "|" + target;
        CaseOriginalTranslationVO cached = translationCache.get(cacheKey);
        if (cached != null) {
            log.info("原文翻译命中缓存 url={} lang={}", url, target);
            return cached;
        }

        String fullText = fetchOriginalText(url);
        CaseOriginalTranslationVO vo = buildTranslationVo(fullText, target, countryOf(caseInfo));
        translationCache.put(cacheKey, vo);
        return vo;
    }

    private String fetchOriginalText(String url) {
        String cached = originalDocumentCacheService.getCachedText(url);
        if (StringUtils.isNotBlank(cached)) {
            return cached;
        }
        originalDocumentCacheService.prefetch(url);
        String fetched = originalDocumentCacheService.fetchOrGet(url, 30, TimeUnit.SECONDS);
        if (StringUtils.isNotBlank(fetched)) {
            return fetched;
        }
        throw new ServiceException("原文尚未抓取到正文，请先打开一次原文或生成 AI 摘要后再翻译");
    }

    private CaseOriginalTranslationVO buildTranslationVo(String fullText, String target, String country) {
        CaseOriginalTranslationVO vo = new CaseOriginalTranslationVO();
        vo.setLanguage(target);

        String normalized = fullText == null ? "" : fullText.trim();
        if (StringUtils.isBlank(normalized)) {
            vo.setContent("");
            vo.setSourceLanguage("auto");
            vo.setTruncated(false);
            vo.setSegmentCount(0);
            vo.setMessage("原文为空，无法翻译");
            return vo;
        }

        String source = detectSourceLanguage(normalized, country);
        vo.setSourceLanguage(source);

        List<String> paragraphs = segmentTextForTranslation(normalized, MAX_SEGMENT_CHARS);
        boolean truncated = false;
        List<String> targetSegments = new ArrayList<>();
        int totalChars = 0;

        boolean translateNeeded = !isSameLanguage(source, target);

        for (String paragraph : paragraphs) {
            if (totalChars + paragraph.length() > MAX_TOTAL_CHARS) {
                truncated = true;
                break;
            }
            totalChars += paragraph.length();
            if (translateNeeded) {
                targetSegments.add(translateChunk(paragraph, source, target));
            } else {
                targetSegments.add(paragraph);
            }
        }

        vo.setContent(String.join("\n\n", targetSegments));
        vo.setTruncated(truncated);
        vo.setSegmentCount(targetSegments.size());
        if (!translateNeeded) {
            vo.setMessage("原文已是" + languageDisplayName(target) + "，无需翻译，已按段落整理返回");
        } else if (truncated) {
            vo.setMessage("原文较长，已按前 " + vo.getSegmentCount() + " 段翻译，其余内容请参考官方原文");
        } else {
            vo.setMessage("译文由大模型生成，仅供学习参考，如与官方原文冲突以原文为准");
        }
        return vo;
    }

    private String translateChunk(String paragraph, String source, String target) {
        String sourceName = languageNameForPrompt(source);
        String targetName = LANG_EN.equals(target) ? "English" : "Simplified Chinese";
        try {
            String translated = springAIService.translate(paragraph, sourceName, targetName);
            return StringUtils.isBlank(translated) ? paragraph : translated.trim();
        } catch (Exception e) {
            log.warn("原文段落翻译失败，保留原文: {}", e.getMessage());
            return paragraph;
        }
    }

    private String countryOf(CaseInfo caseInfo) {
        if (caseInfo == null || caseInfo.getSourceId() == null) {
            return null;
        }
        return CountryEnum.getCodeBySourceId(caseInfo.getSourceId());
    }

    private static boolean isSameLanguage(String source, String target) {
        return LANG_ZH.equals(source) && LANG_ZH.equals(target)
                || LANG_EN.equals(source) && LANG_EN.equals(target);
    }

    private static String languageNameForPrompt(String source) {
        if (LANG_ZH.equals(source)) {
            return "Simplified Chinese";
        }
        if (LANG_EN.equals(source)) {
            return "English";
        }
        if ("ja".equals(source)) {
            return "Japanese";
        }
        // 交给模型自动识别原文语言
        return null;
    }

    private static String languageDisplayName(String code) {
        if (LANG_EN.equals(code)) {
            return "英文";
        }
        return "中文";
    }

    /**
     * 识别正文主要语言：含较多汉字/假名时优先识别中文/日文；美国数据源推断英文；
     * 欧盟等不确定来源返回 auto，交由模型自动识别，避免漏翻。
     */
    static String detectSourceLanguage(String text, String country) {
        if (StringUtils.isBlank(text)) {
            return "auto";
        }
        long cjk = 0;
        long kana = 0;
        long total = 0;
        int sample = Math.min(text.length(), 8000);
        for (int i = 0; i < sample; i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                total++;
                if (ch >= '\u4e00' && ch <= '\u9fff') {
                    cjk++;
                } else if ((ch >= '\u3040' && ch <= '\u309f') || (ch >= '\u30a0' && ch <= '\u30ff')) {
                    kana++;
                }
            }
        }
        if (total == 0) {
            return CountryEnum.JPN.getCode().equals(country) ? "ja"
                    : CountryEnum.US.getCode().equals(country) ? LANG_EN : "auto";
        }
        double kanaRatio = (double) kana / total;
        double cjkRatio = (double) cjk / total;
        if (kanaRatio > 0.05) {
            return "ja";
        }
        if (cjkRatio > 0.35) {
            return LANG_ZH;
        }
        if (CountryEnum.JPN.getCode().equals(country)) {
            return "ja";
        }
        // 美国数据源正文基本为英文；欧盟等站点语言不固定，交由模型自动识别，避免漏翻
        return CountryEnum.US.getCode().equals(country) ? LANG_EN : "auto";
    }

    /**
     * 段落化 + 超长段落断句：
     * 先按空行切出自然段，再合并过短的相邻段；若一段仍超过 maxChars，则按句号/问号/叹号等边界拆成多段。
     * 该方法保持纯函数，便于单元测试。
     */
    static List<String> segmentTextForTranslation(String raw, int maxChars) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isBlank(raw)) {
            return result;
        }
        String normalized = raw.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);

        // 第一阶段：按空行聚合自然段
        List<String> units = new ArrayList<>();
        StringBuilder unit = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                if (unit.length() > 0) {
                    units.add(unit.toString().trim());
                    unit.setLength(0);
                }
                continue;
            }
            if (unit.length() > 0) {
                unit.append(' ');
            }
            unit.append(t);
        }
        if (unit.length() > 0) {
            units.add(unit.toString().trim());
        }

        // 第二阶段：合并过短的相邻段，避免为一行标题单独调用模型
        List<String> merged = new ArrayList<>();
        for (String u : units) {
            if (!merged.isEmpty() && merged.get(merged.size() - 1).length() < 260) {
                String last = merged.remove(merged.size() - 1);
                merged.add((last + " " + u).trim());
            } else {
                merged.add(u);
            }
        }

        // 第三阶段：超长段落按句子边界拆分
        for (String m : merged) {
            if (m.length() <= maxChars) {
                result.add(m);
            } else {
                result.addAll(splitLongParagraph(m, maxChars));
            }
        }
        return result;
    }

    private static List<String> splitLongParagraph(String text, int maxChars) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int len = text.length();
        while (start < len) {
            if (len - start <= maxChars) {
                parts.add(text.substring(start).trim());
                break;
            }
            int end = findSplitIndex(text, start, maxChars);
            parts.add(text.substring(start, end).trim());
            start = end;
        }
        return parts;
    }

    private static int findSplitIndex(String text, int start, int maxChars) {
        int hardEnd = Math.min(text.length(), start + maxChars);
        // 在窗口后 1/3 内找最后一个句子边界
        int searchStart = start + maxChars * 2 / 3;
        for (int i = hardEnd - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '。' || c == '！' || c == '？' || c == '；' || c == ';') {
                int after = i + 1;
                while (after < text.length() && text.charAt(after) == ' ') {
                    after++;
                }
                return after;
            }
        }
        // 无句号边界则找最后一个空格
        for (int i = hardEnd - 1; i >= searchStart; i--) {
            if (text.charAt(i) == ' ') {
                return i + 1;
            }
        }
        return hardEnd;
    }
}
