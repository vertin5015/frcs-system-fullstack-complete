package com.hnu.legal_cases.dto.cases;

import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.enums.LanguageEnum;
import com.hnu.legal_cases.enums.PeriodEnum;
import lombok.Data;

/**
 * 搜索案例入参VO
 *
 * @author baixu
 * @date 2025/7/8
 */
@Data
public class SearchCasesReqVO {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 自然语言形式的搜索关键词
     */
    private String keyword;
    /**
     * 需要返回的语言
     * @see LanguageEnum
     */
    private String language;
    /**
     * 国家，默认为空串查询全部
     * @see CountryEnum
     */
    private String country;
    /**
     * 案例判决时间，默认为空串查询全部
     * @see PeriodEnum
     */
    private Integer period;
    /**
     * 可选：限定数据源，逗号分隔国家代码，如 US,EU,JPN；为空则按 country 逻辑
     */
    private String sources;
    /**
     * 页码
     */
    private Integer pagenum = 1;
    /**
     * 页大小
     */
    private Integer pagesize = 10;

    public int getStartIndex() {
        if (this.pagenum > 0 && this.pagesize > 0) {
            return (this.pagesize * (this.pagenum - 1));
        }
        return 0;
    }

    public int getEndIndex() {
        if (this.pagenum > 0 && this.pagesize > 0) {
            return this.getStartIndex() + this.pagesize - 1;
        }
        return 0;
    }
}
