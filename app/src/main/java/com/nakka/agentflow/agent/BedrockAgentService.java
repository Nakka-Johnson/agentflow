package com.nakka.agentflow.agent;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.nakka.agentflow.api.dto.AgentRunRequest;
import com.nakka.agentflow.api.dto.AgentRunResponse;
import com.nakka.agentflow.llm.BedrockClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agentflow.agent.implementation", havingValue = "bedrock")
public class BedrockAgentService implements AgentService {

    private final BedrockClient bedrockClient;

    @Override
    public AgentRunResponse run(AgentRunRequest request) {
        long start = System.currentTimeMillis();

        String response = bedrockClient.generate(request.query());

        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        long latencyMs = System.currentTimeMillis() - start;

        log.info("Agent run completed: conversationId={}, latencyMs={}",
                conversationId, latencyMs);

        return new AgentRunResponse(response, conversationId, List.of(), latencyMs);
    }
}