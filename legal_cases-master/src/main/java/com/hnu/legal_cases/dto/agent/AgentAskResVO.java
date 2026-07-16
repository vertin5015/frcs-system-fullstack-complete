package com.hnu.legal_cases.dto.agent;

import com.hnu.legal_cases.dto.cases.CaseBaseInfo;
import com.hnu.legal_cases.dto.kb.KbQueryResVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AgentAskResVO {
    private String answer;
    private String route;
    private Integer kbHitCount = 0;
    private Integer searchTotalCount = 0;
    private List<CaseBaseInfo> relatedCases = new ArrayList<>();
    private List<KbQueryResVO.Hit> kbHits = new ArrayList<>();
    private List<TraceStep> trace = new ArrayList<>();

    @Data
    public static class TraceStep {
        private String name;
        private String status;
        private String detail;

        public static TraceStep of(String name, String status, String detail) {
            TraceStep step = new TraceStep();
            step.setName(name);
            step.setStatus(status);
            step.setDetail(detail);
            return step;
        }
    }
}
