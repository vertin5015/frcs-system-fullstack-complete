package com.hnu.legal_cases.dto.kb;

import lombok.Data;

import java.util.List;

@Data
public class KbQueryResVO {
    private String answer;
    private Integer hitCount;
    private List<Hit> hits;

    @Data
    public static class Hit {
        private String chunkId;
        private String sourceId;
        private String title;
        private Double score;
        private String preview;
    }
}
