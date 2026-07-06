package com.hnu.legal_cases.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 语言枚举
 *
 * @author baixu
 * @date 2025/8/17
 */
@Getter
@AllArgsConstructor
public enum LanguageEnum {
    /**
     * 中文
     */
    ZH_CN("zh", "中文"),
    /**
     * 英文
     */
    EN_US("en", "英文");

    private final String code;
    private final String description;

    /**
     * 根据语言代码获取枚举值
     *
     * @param code 语言代码
     * @return 对应的枚举值，如果找不到返回null
     */
    public static LanguageEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (LanguageEnum language : values()) {
            if (language.getCode().equals(code)) {
                return language;
            }
        }
        return null;
    }

    /**
     * 检查语言是否有效
     *
     * @param code 语言
     * @return true 有效 false 无效
     */
    public static Boolean checkValidCode(String code) {
        if (code == null) {
            return false;
        }
        for (LanguageEnum languageEnum : LanguageEnum.values()) {
            if (languageEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
