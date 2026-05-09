package com.nakka.agentflow.api;

import com.nakka.agentflow.api.dto.AgentRunRequest;
import com.nakka.agentflow.api.dto.AgentRunResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agent")
public class AgentController {

    @PostMapping("/run")
    public AgentRunResponse run(@Valid @RequestBody AgentRunRequest request) {
        long start = System.currentTimeMillis();

        // TODO: replace with real orchestrator call once Bedrock is wired up
        String stubResponse = "Received query: " + request.query()
                + " (orchestrator not yet implemented)";

        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        long latencyMs = System.currentTimeMillis() - start;

        return new AgentRunResponse(
                stubResponse,
                conversationId,
                List.of(),
                latencyMs
        );
    }
}