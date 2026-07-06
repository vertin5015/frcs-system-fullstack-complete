package com.hnu.legal_cases.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 判决时期枚举
 *
 * @author baixu
 * @date 2025/8/23
 */
@Getter
@AllArgsConstructor
public enum PeriodEnum {
    /**
     * 最近一年
     */
    ONE_YEAR(1, "最近一年","2025/01/01"),
    /**
     * 最近三年
     */
    THREE_YEARS(3, "最近三年","2023/01/01"),
    /**
     * 最近五年
     */
    FIVE_YEARS(5, "最近五年","2021/01/01"),
    /**
     * 最近十年
     */
    TEN_YEARS(10, "最近十年","2016/01/01");

    private final Integer year;
    private final String description;
    private final String startDate;

    /**
     * 检查国家是否有效
     */
    public static Boolean checkValidCode(Integer year){
        if (year == null) {
            return false;
        }
        for (PeriodEnum periodEnum : PeriodEnum.values()) {
            if (periodEnum.getYear().equals(year)) {
                return true;
            }
        }
        return false;
    }

    public static String getStartDateByYear(Integer year){
        for (PeriodEnum periodEnum : PeriodEnum.values()) {
            if (periodEnum.getYear().equals(year)) {
                return periodEnum.getStartDate();
            }
        }
        return null;
    }

}
