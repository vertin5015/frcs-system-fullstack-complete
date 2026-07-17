package com.hnu.legal_cases.controller;

import com.hnu.legal_cases.dto.agent.AgentAskReqVO;
import com.hnu.legal_cases.dto.agent.AgentAskResVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.LegalAgentService;
import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final LegalAgentService legalAgentService;

    @PostMapping("/ask")
    public JSONReturnBean<AgentAskResVO> ask(@RequestBody AgentAskReqVO reqVO) {
        try {
            log.info("agent ask request question={} refreshCases={}",
                    reqVO == null ? null : reqVO.getQuestion(),
                    reqVO == null ? null : reqVO.getRefreshCases());
            return JSONReturnBean.success(legalAgentService.ask(reqVO));
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("agent ask failed", e);
            return JSONReturnBean.failed("Agent 问答失败");
        }
    }
}
