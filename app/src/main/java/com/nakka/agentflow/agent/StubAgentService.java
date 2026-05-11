package com.nakka.agentflow.agent;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.nakka.agentflow.api.dto.AgentRunRequest;
import com.nakka.agentflow.api.dto.AgentRunResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agentflow.agent.implementation", havingValue = "stub", matchIfMissing = true)
public class StubAgentService implements AgentService {

    private final Orchestrator orchestrator;

    @Override
    public AgentRunResponse run(AgentRunRequest request) {
        long start = System.currentTimeMillis();

        Orchestrator.OrchestratorResult result = orchestrator.run(request.query());

        List<AgentRunResponse.ToolCall> toolCalls = result.toolInvocations().stream()
                .map(inv -> new AgentRunResponse.ToolCall(inv.toolName(), inv.summary()))
                .toList();

        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        long latencyMs = System.currentTimeMillis() - start;

        return new AgentRunResponse(result.response(), conversationId, toolCalls, latencyMs);
    }
}