package com.hnu.legal_cases.dto.cases;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SSE event {@code part} 的载荷：某数据源先返回时的增量列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchStreamPartDTO {
    private String source;
    private List<SearchPreviewCaseDTO> cases;
}
