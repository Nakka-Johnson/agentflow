package com.nakka.agentflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRunRequest(
        @NotBlank @Size(max = 4000) String query,
        String conversationId
) {}