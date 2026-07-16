package com.hnu.legal_cases.service;

import com.hnu.legal_cases.dto.agent.AgentAskReqVO;
import com.hnu.legal_cases.dto.agent.AgentAskResVO;

public interface LegalAgentService {
    AgentAskResVO ask(AgentAskReqVO reqVO);
}
