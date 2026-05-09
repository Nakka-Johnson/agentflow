package com.nakka.agentflow.api.dto;

import java.util.List;

public record AgentRunResponse(
        String response,
        String conversationId,
        List<ToolCall> toolCalls,
        long latencyMs
) {
    public record ToolCall(String toolName, String summary) {}
}