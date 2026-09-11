package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dto.ai.SpringAIResVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.SpringAIService;
import com.hnu.legal_cases.service.AiCallRunner;
import com.hnu.legal_cases.service.SummaryAiCallRunner;
import com.alibaba.fastjson.JSON;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final AiCallRunner aiCallRunner;

    @Autowired
    private SummaryAiCallRunner summaryAiCallRunner;

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

        // 中文法律词优先走本地词典，避免上游模型返回空内容时把中文原样送给英文案例库。
        if (containsCjk(trimmed)) {
            String local = fallbackCnKeywordForCrawler(trimmed);
            if (!local.equals(trimmed) && !containsCjk(local)) {
                log.info("中文关键词本地转换生效：{} -> {}", trimmed, local);
                return local;
            }
        }

        // 当前 US/EU/JPN 三个数据源都使用英文检索（JPN 源为最高裁英文判例页），
        // 因此关键词统一提取为英文，避免中文/日文关键词命中不到英文判例。
        String language = "English";

        try {
            String extractPrompt = String.format(
                    EXTRACT_PROMPT_TEMPLATE,
                    trimmed,
                    language
            );

            SpringAIResVO res = aiCallRunner.call(() -> keywordExtractionClient.prompt()
                    .user(extractPrompt)
                    .call()
                    .entity(SpringAIResVO.class));

            if (res != null && "ok".equals(res.getStatus()) && StringUtils.isNotBlank(res.getResult())) {
                log.info("ai提取关键词成功：{}", res.getResult());
                return res.getResult().trim();
            }
            log.warn("ai返回非ok或result为空，改本地兜底：{}", trimmed);
            return fallbackCnKeywordForCrawler(trimmed);
        } catch (Exception e) {
            // 中文等关键词易导致模型输出非严格 JSON 或解析失败，此前会触发搜索接口 Throwable 分支仅提示「搜索案例错误」
            log.warn("ai提取关键词异常，改用原始关键词继续搜索：{}", trimmed, e);
            String fallback = fallbackCnKeywordForCrawler(trimmed);
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

            String raw = summaryAiCallRunner.call(() -> summaryClient.prompt()
                    .user(summaryPrompt)
                    .call()
                    .content());

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
            String raw = aiCallRunner.call(() -> summaryClient.prompt()
                    .user(qaPrompt)
                    .call()
                    .content());
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
            String raw = aiCallRunner.call(() -> summaryClient.prompt()
                    .user(prompt)
                    .call()
                    .content());
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
            
            你是法律案例检索的关键词提取器。请从用户查询中提取最多 3 个英文法律检索关键词或准确法律短语。
            要求：
            - 关键词必须简短、适合搜索引擎和官方案例库检索
            - 删除代词、案号、日期、程序性词语和无关描述
            - 如果原文不是英文，请先翻译成英文法律术语
            - 关键词间用一个空格分隔
            - 输出JSON格式，类似：
            {
                "status": "ok",
                "result": "keyword1 keyword2"
            }
            "result"中最多保留 3 个关键词。
            
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
            The first section MUST be titled "案件基本信息" and include these exact labeled lines:
            - 案件名称：
            - 案号：
            - 判决时间：
            - 判决法庭：
            - 当事人：
            - 简要内容：
            Then continue with 关键词、基本案情、裁判理由、裁判要旨、关联索引.
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

    private static String fallbackCnKeywordForCrawler(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return keyword;
        }
        if (!containsCjk(keyword)) {
            return keyword;
        }
        String s = keyword;
        Map<String, String> map = new LinkedHashMap<>();
        map.put("抢劫罪", "robbery");
        map.put("抢劫", "robbery");
        map.put("夫妻财产", "marital property");
        map.put("婚姻财产", "marital property");
        map.put("夫妻共同财产", "community property");
        map.put("工人解雇", "worker dismissal");
        map.put("解雇赔偿", "unfair dismissal compensation");
        map.put("劳动纠纷", "labor dispute");
        map.put("劳动合同", "employment contract");
        map.put("解雇", "dismissal");
        map.put("辞退", "dismissal");
        map.put("工伤", "work injury");
        map.put("工人", "worker");
        map.put("雇员", "employee");
        map.put("雇主", "employer");
        map.put("公司", "company");
        map.put("赔偿", "compensation");
        map.put("抚养权", "child custody");
        map.put("盗窃罪", "theft");
        map.put("盗窃", "theft");
        map.put("故意杀人", "murder");
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

    /**
     * Detects the local fallback summaries produced by this class so callers can avoid persisting
     * them as successful AI-generated summaries.
     */
    public static boolean isFallbackSummary(String content) {
        if (StringUtils.isBlank(content)) {
            return true;
        }
        String normalized = content.trim();
        return normalized.contains("本地兜底")
                || normalized.contains("Case Summary (Fallback)")
                || normalized.contains("Fallback)");
    }

    private static boolean isSimpleLatinKeyword(String s) {
        return s.length() <= 80
                && s.matches("[A-Za-z0-9][A-Za-z0-9 '\\-&,./]*")
                && !containsCjk(s);
    }
}
