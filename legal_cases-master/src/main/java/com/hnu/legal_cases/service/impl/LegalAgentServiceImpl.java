package com.hnu.legal_cases.service.impl;

import com.hnu.legal_cases.dto.agent.AgentAskReqVO;
import com.hnu.legal_cases.dto.agent.AgentAskResVO;
import com.hnu.legal_cases.dto.cases.CaseBaseInfo;
import com.hnu.legal_cases.dto.cases.SearchCasesReqVO;
import com.hnu.legal_cases.dto.cases.SearchCasesResVO;
import com.hnu.legal_cases.dto.kb.KbQueryReqVO;
import com.hnu.legal_cases.dto.kb.KbQueryResVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.LegalAgentService;
import com.hnu.legal_cases.service.LocalKbService;
import com.hnu.legal_cases.service.SearchCasesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LegalAgentServiceImpl implements LegalAgentService {

    private final SearchCasesService searchCasesService;
    private final LocalKbService localKbService;

    @Override
    public AgentAskResVO ask(AgentAskReqVO reqVO) {
        validate(reqVO);

        String question = reqVO.getQuestion().trim();
        String language = normalizeLanguage(reqVO.getLanguage());
        int topK = normalizeTopK(reqVO.getTopK());

        AgentAskResVO res = new AgentAskResVO();
        boolean refreshCases = Boolean.TRUE.equals(reqVO.getRefreshCases());
        res.setRoute(refreshCases ? "search_then_rag" : "rag_only");
        res.getTrace().add(AgentAskResVO.TraceStep.of("plan", "done",
                refreshCases ? "refresh cases before KB retrieval" : "query local KB directly"));

        if (refreshCases) {
            SearchCasesResVO searchRes = searchCases(reqVO, question, language);
            int totalCount = searchRes.getTotalCount() == null ? 0 : searchRes.getTotalCount();
            List<CaseBaseInfo> cases = searchRes.getCases() == null ? List.of() : searchRes.getCases();
            res.setSearchTotalCount(totalCount);
            res.setRelatedCases(limitCases(cases, 5));
            res.getTrace().add(AgentAskResVO.TraceStep.of("case_search", "done",
                    "matched " + totalCount + " cases"));
        }

        KbQueryResVO kbRes = queryKnowledgeBase(question, language, topK);
        int hitCount = kbRes.getHitCount() == null ? 0 : kbRes.getHitCount();
        res.setKbHitCount(hitCount);
        res.setKbHits(kbRes.getHits() == null ? List.of() : kbRes.getHits());
        res.getTrace().add(AgentAskResVO.TraceStep.of("kb_retrieve", "done",
                "retrieved " + hitCount + " KB chunks"));

        res.setAnswer(buildAnswer(question, kbRes.getAnswer(), hitCount, res.getSearchTotalCount()));
        res.getTrace().add(AgentAskResVO.TraceStep.of("answer", "done", "returned RAG answer"));
        return res;
    }

    private SearchCasesResVO searchCases(AgentAskReqVO reqVO, String question, String language) {
        SearchCasesReqVO searchReq = new SearchCasesReqVO();
        searchReq.setUserId(reqVO.getUserId());
        searchReq.setKeyword(question);
        searchReq.setLanguage(language);
        searchReq.setCountry(reqVO.getCountry());
        searchReq.setSources(reqVO.getSources());
        searchReq.setPeriod(reqVO.getPeriod());
        searchReq.setPagenum(1);
        searchReq.setPagesize(5);
        SearchCasesResVO searchRes = searchCasesService.searchCases(searchReq);
        return searchRes == null ? new SearchCasesResVO() : searchRes;
    }

    private KbQueryResVO queryKnowledgeBase(String question, String language, int topK) {
        KbQueryReqVO kbReq = new KbQueryReqVO();
        kbReq.setQuestion(question);
        kbReq.setLanguage(language);
        kbReq.setTopK(topK);
        KbQueryResVO kbRes = localKbService.query(kbReq);
        return kbRes == null ? new KbQueryResVO() : kbRes;
    }

    private String buildAnswer(String question, String kbAnswer, int hitCount, int searchTotalCount) {
        String answer = StringUtils.hasText(kbAnswer) ? kbAnswer.trim() : "知识库暂未检索到可用答案，请先导入案例或法规材料。";
        return "问题：" + question
                + "\n\n回答：" + answer
                + "\n\n检索依据：知识库命中 " + hitCount + " 条；案例搜索命中 " + searchTotalCount + " 条。";
    }

    private List<CaseBaseInfo> limitCases(List<CaseBaseInfo> cases, int limit) {
        if (cases == null || cases.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(cases.subList(0, Math.min(limit, cases.size())));
    }

    private String normalizeLanguage(String language) {
        return StringUtils.hasText(language) ? language.trim() : "zh";
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return 5;
        }
        return Math.max(1, Math.min(topK, 10));
    }

    private void validate(AgentAskReqVO reqVO) {
        if (reqVO == null || !StringUtils.hasText(reqVO.getQuestion())) {
            throw new ServiceException("问题不能为空");
        }
    }
}
