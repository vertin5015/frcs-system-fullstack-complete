package com.hnu.legal_cases.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 国家枚举
 *
 * @author baixu
 * @date 2025/8/23
 */
@Getter
@AllArgsConstructor
public enum CountryEnum {
    /**
     * 美国
     */
    US("US", 0),
    /**
     * 欧盟
     */
    EU("EU", 1),
    /**
     * 日本
     */
    JPN("JPN", 2);

    private final String code;
    private final Integer sourceId;

    /**
     * 根据 sourceId 获取 code
     *
     * @param sourceId 数据源ID
     * @return 国家/地区代码
     */
    public static String getCodeBySourceId(Integer sourceId) {
        for (CountryEnum countryEnum : CountryEnum.values()) {
            if (countryEnum.getSourceId().equals(sourceId)) {
                return countryEnum.getCode();
            }
        }
        return null;
    }

    /**
     * 根据 code 获取 sourceId
     *
     * @param code 国家代码
     * @return 数据源ID
     */
    public static Integer getSourceIdByCode(String code) {
        for (CountryEnum countryEnum : CountryEnum.values()) {
            if (countryEnum.getCode().equals(code)) {
                return countryEnum.getSourceId();
            }
        }
        return null;
    }

    /**
     * 检查国家是否有效
     */
    public static Boolean checkValidCode(String code) {
        if (code == null) {
            return false;
        }
        for (CountryEnum countryEnum : CountryEnum.values()) {
            if (countryEnum.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

}
