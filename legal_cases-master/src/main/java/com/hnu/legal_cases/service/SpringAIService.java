package com.hnu.legal_cases.service;

public interface SpringAIService {

    /**
     * 提取关键词
     *
     * @param keyword  自然语言
     * @param language 语言
     * @return 特定语言的关键词
     */
    String extractKeyword(String keyword, String language);

    /**
     * 总结案例详细信息
     *
     * @param content 案例详细信息
     * @return 案例总结
     */
    String summaryCase(String content);

    String summaryCase(String content, String language);

    /**
     * 基于已生成的案例摘要回答用户提问
     */
    String answerCaseQuestion(String caseSummary, String question, String language);

    /**
     * 使用配置的 AI 模型翻译文本（不依赖百度等第三方翻译接口）。
     *
     * @param text            原文
     * @param sourceLanguage  源语言说明（如 English、Japanese）
     * @param targetLanguage  目标语言说明（如 Simplified Chinese）
     * @return 译文；失败时返回原文 trim 后的内容
     */
    String translate(String text, String sourceLanguage, String targetLanguage);
}
