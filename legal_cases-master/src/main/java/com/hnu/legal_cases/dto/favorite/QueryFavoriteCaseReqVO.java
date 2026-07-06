package com.hnu.legal_cases.dto.favorite;

import com.hnu.legal_cases.enums.CountryEnum;
import com.hnu.legal_cases.enums.LanguageEnum;
import com.hnu.legal_cases.enums.PeriodEnum;
import lombok.Data;

@Data
public class QueryFavoriteCaseReqVO {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 需要返回的语言
     * @see LanguageEnum
     */
    private  String language;
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
            return this.getStartIndex() + this.pagesize;
        }
        return 0;
    }
}
