package com.hnu.legal_cases.dao;

import com.hnu.legal_cases.pojo.CaseDetailInfo;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * case_detail_info 表对应 Mapper
 */
public interface CaseDetailMapper {

    void insert(CaseDetailInfo caseDetailInfo);

    CaseDetailInfo getCaseDetailByCaseId(@Param("caseId") String caseId);

    void insertPlaceholder(@Param("caseId") String caseId);

    int updateSummaryStatus(@Param("caseId") String caseId,
                            @Param("status") String status,
                            @Param("error") String error);

    int updateSummaryDone(@Param("caseId") String caseId,
                          @Param("contentZhCn") String contentZhCn,
                          @Param("contentEnUs") String contentEnUs);

    int resetSummary(@Param("caseId") String caseId);

    List<CaseDetailInfo> listRecentForKb(@Param("limit") Integer limit);
}
