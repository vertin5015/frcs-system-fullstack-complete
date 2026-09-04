package com.hnu.legal_cases.dto.kb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Diagnostic response for the configured embedding pipeline.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingHealthResVO {

    private String source;

    private String model;

    private Integer dimensions;

    private Long latencyMs;
}
