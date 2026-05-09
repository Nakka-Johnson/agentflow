package com.nakka.agentflow.agent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.nakka.agentflow.api.dto.AgentRunRequest;
import com.nakka.agentflow.api.dto.AgentRunResponse;
import com.nakka.agentflow.tools.DocSearchTool;
import com.nakka.agentflow.tools.Tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agentflow.agent.implementation", havingValue = "stub", matchIfMissing = true)
public class StubAgentService implements AgentService {

    private final DocSearchTool docSearchTool;

    @Override
    public AgentRunResponse run(AgentRunRequest request) {
        long start = System.currentTimeMillis();

        Tool.ToolResult retrieval = docSearchTool.execute(Map.of("query", request.query()));

        String response;
        List<AgentRunResponse.ToolCall> toolCalls;

        if (retrieval.success()) {
            response = "Stub response. Retrieval found relevant context. "
                    + "When Bedrock is wired up, this content will be passed to Claude.";
            toolCalls = List.of(
                    new AgentRunResponse.ToolCall(docSearchTool.name(), retrieval.summary())
            );
        } else {
            response = "Stub response. Retrieval failed: " + retrieval.summary();
            toolCalls = List.of();
        }

        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        long latencyMs = System.currentTimeMillis() - start;

        return new AgentRunResponse(response, conversationId, toolCalls, latencyMs);
    }
}