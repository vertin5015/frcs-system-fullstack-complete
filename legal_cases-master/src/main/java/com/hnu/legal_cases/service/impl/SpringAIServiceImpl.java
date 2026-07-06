package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dto.ai.SpringAIResVO;
import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.SpringAIService;
import com.alibaba.fastjson.JSON;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAIServiceImpl implements SpringAIService {

    private final ChatClient keywordExtractionClient;

    private final ChatClient summaryClient;

    /**
     * 提取关键词
     *
     * @param keyword 自然语言
     * @param country 语言
     * @return 特定语言的关键词
     */
    @Override
    public String extractKeyword(String keyword, String country) {
        if (StringUtils.isBlank(keyword)) {
            throw new ServiceException("关键词为空");
        }
        final String trimmed = keyword.trim();

        if (isSimpleLatinKeyword(trimmed)) {
            log.info("英文关键词跳过 AI 提取：{}", trimmed);
            return trimmed;
        }

        String language = "English";
        if (CountryEnum.JPN.getCode().equals(country)) {
            language = "Japanese";
        }

        try {
            String extractPrompt = String.format(
                    EXTRACT_PROMPT_TEMPLATE,
                    trimmed,
                    language
            );

            SpringAIResVO res = keywordExtractionClient.prompt()
                    .user(extractPrompt)
                    .call()
                    .entity(SpringAIResVO.class);

            if (res != null && "ok".equals(res.getStatus()) && StringUtils.isNotBlank(res.getResult())) {
                log.info("ai提取关键词成功：{}", res.getResult());
                return res.getResult().trim();
            }
            log.warn("ai返回非ok或result为空，改用原始关键词：{}", trimmed);
            return trimmed;
        } catch (Exception e) {
            // 中文等关键词易导致模型输出非严格 JSON 或解析失败，此前会触发搜索接口 Throwable 分支仅提示「搜索案例错误」
            log.warn("ai提取关键词异常，改用原始关键词继续搜索：{}", trimmed, e);
            String fallback = fallbackCnKeywordForCrawler(trimmed, country);
            if (!fallback.equals(trimmed)) {
                log.info("关键词本地兜底翻译生效：{} -> {}", trimmed, fallback);
            }
            return fallback;
        }
    }

    /**
     * 总结案例详细信息
     *
     * @param content 案例详细信息
     * @return 案例总结
     */
    @Override
    public String summaryCase(String content) {
        return summaryCase(content, "en");
    }

    @Override
    public String summaryCase(String content, String language) {
        String safeContent = content == null ? "" : content;
        boolean zh = language != null && language.toLowerCase().startsWith("zh");
        try {
            String summaryPrompt = String.format(
                    SUMMARY_PROMPT_TEMPLATE,
                    zh ? "Simplified Chinese" : "English",
                    safeContent
            );

            String raw = summaryClient.prompt()
                    .user(summaryPrompt)
                    .call()
                    .content();

            String result = unwrapAiResult(raw);
            if (StringUtils.isNotBlank(result)) {
                log.info("ai总结案例详细信息成功");
                return result;
            }
            throw new IllegalStateException("ai返回空结果");
        } catch (Exception e) {
            // 免费模式兜底：上游模型额度不足或临时失败时，返回本地抽取式摘要，避免前端一直提示充值。
            log.warn("ai总结失败，改用本地摘要兜底: {}", e.getMessage());
            return buildFallbackSummary(safeContent, zh);
        }
    }

    @Override
    public String answerCaseQuestion(String caseSummary, String question, String language) {
        String langLine = language != null && language.startsWith("zh") ? "中文" : "English";
        String qaPrompt = String.format(
                QA_PROMPT_TEMPLATE,
                caseSummary,
                question,
                langLine
        );
        try {
            String raw = summaryClient.prompt()
                    .user(qaPrompt)
                    .call()
                    .content();
            String result = unwrapAiResult(raw);
            if (StringUtils.isNotBlank(result)) {
                log.info("ai 本案问答成功");
                return result;
            }
        } catch (Exception e) {
            log.warn("ai 问答失败，使用本地兜底: {}", e.getMessage());
        }
        return buildFallbackQa(caseSummary, question, language);
    }

    @Override
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        final String trimmed = text.trim();
        try {
            String prompt = String.format(
                    TRANSLATE_PROMPT_TEMPLATE,
                    sourceLanguage != null ? sourceLanguage : "the source language of the text",
                    targetLanguage != null ? targetLanguage : "Simplified Chinese",
                    trimmed
            );
            String raw = summaryClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            String result = unwrapAiResult(raw);
            if (StringUtils.isNotBlank(result)) {
                return result.trim();
            }
            log.warn("ai翻译返回非ok或为空，使用原文");
            return trimmed;
        } catch (Exception e) {
            log.warn("ai翻译异常，使用原文: {}", e.getMessage());
            return trimmed;
        }
    }

    private static final String TRANSLATE_PROMPT_TEMPLATE = """
            You are a professional legal translator.
            Translate the text below from %s to %s.
            Preserve names, citations, and legal terminology. Output only the translation (no preface).

            Text:
            %s

            Output JSON only (must be valid JSON):
            {"status":"ok","result":"your translation here"}
            """;

    private static final String QA_PROMPT_TEMPLATE = """
            下面是某案件的 AI 摘要（Markdown），请仅根据摘要内容回答用户问题；若摘要中没有相关信息，请说明无法从摘要中判断。
            
            【案件摘要】
            %s
            
            【用户问题】
            %s
            
            【回答要求】
            使用 %s 作答，条理清晰、简洁；输出 JSON 格式：
            {"status":"ok","result":"你的回答"}
            
            请仔细检查是否生成了完整的大括号。
            """;

    private static final String EXTRACT_PROMPT_TEMPLATE = """
            文本内容：%s
            
            目标语言：%s
            
            请提取文本中最重要的关键词并翻译为目标语言。
            要求：
            - 可提取1个或多个关键词，关键词间用空格相隔
            - 翻译要准确
            - 输出JSON格式，类似：
            {
                "status": "ok",
                "result": "keyword1 keyword2"
            }
            "result"中的关键词个数视提取情况而定
            
            请仔细检查是否生成了完整的大括号！
            """;

    private static final String SUMMARY_PROMPT_TEMPLATE = """
            Please analyze and summarize the following legal case information according to a standard structure.
            Please focus on the most important details while keeping your summary concise and within 1000 words.
            Output language: %s.
            
            **Case Content to be Analyzed:**
            %s
            
            Please provide a comprehensive summary according to the standard case summary structure
            and return it in Markdown format using the requested output language.
            Note that plain text is returned directly!
            
            Output Markdown only. Do not wrap the answer in JSON. Do not use ``` code fences.
            """;

    private static String buildFallbackSummary(String content, boolean zh) {
        if (StringUtils.isBlank(content)) {
            return zh
                    ? "## 案件摘要（本地兜底）\n\n原文暂未抓取成功，请稍后点击「重新生成」。当前无法生成有效摘要。"
                    : "## Case Summary (Fallback)\n\nThe original text is not available yet. Please regenerate later.";
        }
        String compact = content.replace("\r", "\n").replaceAll("\\n{2,}", "\n").trim();
        int maxChars = 3800;
        String body = compact.length() > maxChars ? compact.substring(0, maxChars) : compact;
        if (zh) {
            return "## 案件摘要（本地兜底）\n\n"
                    + "- 当前 AI 服务不可用或返回异常，以下为本地抽取式摘要。\n"
                    + "- 原文抓取完整后可再次点击「重新生成」。\n\n"
                    + "### 关键片段\n\n"
                    + body;
        }
        return "## Case Summary (Fallback)\n\n"
                + "- AI service is temporarily unavailable; this is an extractive local summary.\n"
                + "- Please retry later for a full model-generated summary.\n\n"
                + "### Key Excerpts\n\n"
                + body;
    }

    private static String unwrapAiResult(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        String withoutFence = stripMarkdownFence(trimmed);
        try {
            SpringAIResVO vo = JSON.parseObject(withoutFence, SpringAIResVO.class);
            if (vo != null && StringUtils.isNotBlank(vo.getResult())) {
                return vo.getResult().trim();
            }
        } catch (Exception ignore) {
            // 本地模型常返回普通 Markdown，而不是严格 JSON，直接采用原文。
        }
        return withoutFence;
    }

    private static String stripMarkdownFence(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        String s = text.trim();
        if (!s.startsWith("```")) {
            return s;
        }
        int firstBreak = s.indexOf('\n');
        int lastFence = s.lastIndexOf("```");
        if (firstBreak >= 0 && lastFence > firstBreak) {
            return s.substring(firstBreak + 1, lastFence).trim();
        }
        return s;
    }

    private static String buildFallbackQa(String summary, String question, String language) {
        boolean zh = language != null && language.startsWith("zh");
        if (StringUtils.isBlank(summary)) {
            return zh ? "当前摘要为空，无法回答该问题。" : "Summary is empty, unable to answer this question.";
        }
        String compact = summary.replace("\r", "\n").trim();
        int maxLen = 700;
        String excerpt = compact.length() > maxLen ? compact.substring(0, maxLen) + "..." : compact;
        if (zh) {
            return "当前 AI 服务不可用，先基于摘要给你可读回答：\n\n"
                    + "你的问题：" + (question == null ? "" : question) + "\n\n"
                    + "相关摘要片段：\n" + excerpt;
        }
        return "AI service is currently unavailable. Fallback answer based on summary:\n\n"
                + "Question: " + (question == null ? "" : question) + "\n\n"
                + "Relevant excerpt:\n" + excerpt;
    }

    private static String fallbackCnKeywordForCrawler(String keyword, String country) {
        if (StringUtils.isBlank(keyword)) {
            return keyword;
        }
        // JPN 源优先走日文翻译流程，不使用本兜底。
        if (CountryEnum.JPN.getCode().equals(country)) {
            return keyword;
        }
        if (!containsCjk(keyword)) {
            return keyword;
        }
        String s = keyword;
        Map<String, String> map = new LinkedHashMap<>();
        map.put("合同纠纷", "contract dispute");
        map.put("合同", "contract");
        map.put("侵权", "tort");
        map.put("离婚", "divorce");
        map.put("抚养", "custody");
        map.put("刑事", "criminal");
        map.put("杀人", "homicide");
        map.put("谋杀", "murder");
        map.put("诈骗", "fraud");
        map.put("破产", "bankruptcy");
        map.put("商标", "trademark");
        map.put("专利", "patent");
        map.put("版权", "copyright");
        map.put("劳动", "employment");
        map.put("移民", "immigration");
        map.put("证券", "securities");
        for (Map.Entry<String, String> e : map.entrySet()) {
            s = s.replace(e.getKey(), e.getValue());
        }
        String out = s.trim().replaceAll("\\s+", " ");
        return out.isEmpty() ? keyword : out;
    }

    private static boolean containsCjk(String s) {
        for (int i = 0; i < s.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(s.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSimpleLatinKeyword(String s) {
        return s.length() <= 80
                && s.matches("[A-Za-z0-9][A-Za-z0-9 '\\-&,./]*")
                && !containsCjk(s);
    }
}
