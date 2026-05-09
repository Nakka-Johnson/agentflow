package com.nakka.agentflow.agent;

import com.nakka.agentflow.api.dto.AgentRunRequest;
import com.nakka.agentflow.api.dto.AgentRunResponse;

public interface AgentService {

    AgentRunResponse run(AgentRunRequest request);
}