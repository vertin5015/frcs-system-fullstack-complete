package com.hnu.legal_cases.dto.ai;

import lombok.Data;

/**
 * 案例原文（或原文译文）分段结果，供 Web 端案例阅读页展示。
 */
@Data
public class CaseOriginalTranslationVO {
    /**
     * 翻译后（或无需翻译时按段落整理后的）原文正文。
     * 段落之间以空行（\n\n）分隔。
     */
    private String content;

    /**
     * 请求的目标语言：zh / en
     */
    private String language;

    /**
     * 识别出的原文语言：zh / en / ja / auto（auto 表示未明确识别，交由模型自动判断）
     */
    private String sourceLanguage;

    /**
     * 是否因正文过长被截断（仅翻译前 N 段）
     */
    private Boolean truncated;

    /**
     * 实际返回/翻译的段落数
     */
    private Integer segmentCount;

    /**
     * 给前端的附加说明（如“原文已是中文，无需翻译，已按段落整理返回”）
     */
    private String message;
}
