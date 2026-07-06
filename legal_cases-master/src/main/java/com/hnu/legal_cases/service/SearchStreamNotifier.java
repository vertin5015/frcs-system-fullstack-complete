package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.cases.SearchCasesResVO;
import com.hnu.legal_cases.dto.cases.SearchStreamPartDTO;

/**
 * 案例搜索 SSE：先按数据源推送 {@link #part}，最后 {@link #done} 与同步接口一致。
 */
public interface SearchStreamNotifier {

    void part(SearchStreamPartDTO chunk);

    void done(SearchCasesResVO res);

    void fail(String message);
}
