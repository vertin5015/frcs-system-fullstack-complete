package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dto.agent.AgentAskReqVO;
import com.hnu.legal_cases.dto.agent.AgentAskResVO;
import com.hnu.legal_cases.dto.cases.CaseBaseInfo;
import com.hnu.legal_cases.dto.cases.SearchCasesResVO;
import com.hnu.legal_cases.dto.kb.KbQueryResVO;
import com.hnu.legal_cases.service.LocalKbService;
import com.hnu.legal_cases.service.SearchCasesService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegalAgentServiceImplTest {

    @Test
    void askSearchesCasesQueriesKnowledgeBaseAndReturnsTrace() {
        SearchCasesService searchCasesService = mock(SearchCasesService.class);
        LocalKbService localKbService = mock(LocalKbService.class);

        SearchCasesResVO searchRes = new SearchCasesResVO();
        CaseBaseInfo caseInfo = new CaseBaseInfo();
        caseInfo.setCase_id("US-001");
        caseInfo.setCase_name("Forum selection dispute");
        searchRes.setTotalCount(1);
        searchRes.setCases(List.of(caseInfo));
        when(searchCasesService.searchCases(argThat(req ->
                "forum selection clause".equals(req.getKeyword())
                        && "US".equals(req.getSources())
                        && Integer.valueOf(3).equals(req.getPeriod())
        ))).thenReturn(searchRes);

        KbQueryResVO kbRes = new KbQueryResVO();
        kbRes.setAnswer("A forum selection clause is usually reviewed with attention to consent and fairness.");
        kbRes.setHitCount(2);
        when(localKbService.query(argThat(req ->
                "forum selection clause".equals(req.getQuestion())
                        && "zh".equals(req.getLanguage())
                        && Integer.valueOf(5).equals(req.getTopK())
        ))).thenReturn(kbRes);

        LegalAgentServiceImpl service = new LegalAgentServiceImpl(searchCasesService, localKbService);
        AgentAskReqVO req = new AgentAskReqVO();
        req.setQuestion("forum selection clause");
        req.setLanguage("zh");
        req.setSources("US");
        req.setPeriod(3);
        req.setTopK(5);
        req.setRefreshCases(true);

        AgentAskResVO res = service.ask(req);

        assertThat(res.getAnswer()).contains("forum selection clause");
        assertThat(res.getKbHitCount()).isEqualTo(2);
        assertThat(res.getSearchTotalCount()).isEqualTo(1);
        assertThat(res.getRelatedCases()).hasSize(1);
        assertThat(res.getTrace()).extracting(AgentAskResVO.TraceStep::getName)
                .containsExactly("plan", "case_search", "kb_retrieve", "answer");
        verify(searchCasesService).searchCases(argThat(arg -> "forum selection clause".equals(arg.getKeyword())));
        verify(localKbService).query(argThat(arg -> "forum selection clause".equals(arg.getQuestion())));
    }

    @Test
    void askCanUseKnowledgeBaseOnlyForFastDemo() {
        SearchCasesService searchCasesService = mock(SearchCasesService.class);
        LocalKbService localKbService = mock(LocalKbService.class);

        KbQueryResVO kbRes = new KbQueryResVO();
        kbRes.setAnswer("RAG answer from local knowledge base.");
        kbRes.setHitCount(1);
        when(localKbService.query(argThat(req -> "contract breach".equals(req.getQuestion())))).thenReturn(kbRes);

        LegalAgentServiceImpl service = new LegalAgentServiceImpl(searchCasesService, localKbService);
        AgentAskReqVO req = new AgentAskReqVO();
        req.setQuestion("contract breach");
        req.setRefreshCases(false);

        AgentAskResVO res = service.ask(req);

        assertThat(res.getAnswer()).contains("RAG answer");
        assertThat(res.getSearchTotalCount()).isZero();
        assertThat(res.getTrace()).extracting(AgentAskResVO.TraceStep::getName)
                .containsExactly("plan", "kb_retrieve", "answer");
    }
}
