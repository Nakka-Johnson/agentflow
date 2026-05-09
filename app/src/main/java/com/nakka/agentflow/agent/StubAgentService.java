package com.nakka.agentflow.agent;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.nakka.agentflow.api.dto.AgentRunRequest;
import com.nakka.agentflow.api.dto.AgentRunResponse;

@Service
@ConditionalOnProperty(name = "agentflow.agent.implementation", havingValue = "stub", matchIfMissing = true)
public class StubAgentService implements AgentService {

    @Override
    public AgentRunResponse run(AgentRunRequest request) {
        long start = System.currentTimeMillis();

        String response = "Received query: " + request.query()
                + " (stub agent, no LLM yet)";

        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        long latencyMs = System.currentTimeMillis() - start;

        return new AgentRunResponse(response, conversationId, List.of(), latencyMs);
    }
}