package com.hnu.legal_cases.dto.cases;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-source result summary shown to the user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSourceStat {

    private String source;

    private Integer count;

    /**
     * SUCCESS, NO_RESULTS, FAILED, or TIMEOUT.
     */
    private String status;
}
