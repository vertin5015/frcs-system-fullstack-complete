package com.hnu.legal_cases.dao;

import com.hnu.legal_cases.pojo.CaseInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * case_zh_CN 与 case_en_US 表对应 Mapper
 *
 * @author baixu
 * @date 2025/8/31
 */
public interface CaseMapper {

    /**
     * 批量插入英文案例
     */
    void insertEnCases(CaseInfo caseInfo);

    /**
     * 批量插入中文案例
     */
    void insertZhCases(CaseInfo caseInfo);

    /**
     * 根据caseId列表查询已存在的案例
     */
    List<String> queryExistingCaseIds(@Param("caseIds") List<String> caseIds);

    /**
     * 根据caseId列表和语言查询案例
     *
     * @param caseIds  案例ID列表
     * @param language 语言
     * @return 对应语言的案例信息
     */
    List<CaseInfo> queryCases(@Param("caseIds") Set<String> caseIds, @Param("language") String language);

    /**
     * 根据caseId列表、语言、数据源ID、判决日期范围查询案例
     * @param caseIds 案例ID列表
     * @param language 语言
     * @param sourceId 数据源ID
     * @param startDate 判决日期需晚于此值
     * @return 案例信息
     */
    List<CaseInfo> queryCasesBySourceIdAndDate(@Param("caseIds") Set<String> caseIds, @Param("language") String language, @Param("sourceId") Integer sourceId, @Param("startDate") String startDate);
}
