package com.hnu.legal_cases.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kb.local")
public class KbProperties {
    /**
     * 本地索引文件路径（JSON）。
     */
    private String indexFile = "kb-index/local-kb.json";

    /**
     * 单块最大字符数。
     */
    private Integer chunkSize = 800;

    /**
     * 邻接块重叠字符数。
     */
    private Integer chunkOverlap = 120;

    /**
     * 默认召回数量。
     */
    private Integer defaultTopK = 5;

    /**
     * 最大召回数量（防止一次请求过大）。
     */
    private Integer maxTopK = 12;
}
