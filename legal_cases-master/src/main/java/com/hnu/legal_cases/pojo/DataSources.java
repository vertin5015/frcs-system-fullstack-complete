package com.hnu.legal_cases.pojo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DataSources {
    /**
     * 数据源ID
     */
    private Integer sourceId;
    /**
     * 数据源名称
     */
    private String sourceName;
    /**
     * 数据源URL
     */
    private String sourceUrl;
    /**
     * 数据源所属国家
     */
    private String sourceCountry;
    /**
     * 插入时间
     */
    private Timestamp insertTime;
    /**
     * 更新时间
     */
    private Timestamp updateTime;
}
