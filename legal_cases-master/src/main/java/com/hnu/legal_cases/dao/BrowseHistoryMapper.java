package com.hnu.legal_cases.dao;

import com.hnu.legal_cases.pojo.BrowseHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;;

/**
 * browse_history 表对应 Mapper
 *
 * @author baixu
 * @date 2025/9/6
 */
public interface BrowseHistoryMapper {

    /**
     * 插入浏览历史
     */
    void insert(@Param("userId") Long userId, @Param("caseId") String caseId);

    /**
     * 更新浏览历史
     */
    void update(@Param("historyId") Long historyId);

    /**
     * 根据userId和caseId查询单个记录
     */
    BrowseHistory existsByUserIdAndCaseId(@Param("userId") Long userId, @Param("caseId") String caseId);

    /**
     * 根据userId查询浏览历史
     */
    List<BrowseHistory> queryBrowseHistory(@Param("userId") Long userId);

}