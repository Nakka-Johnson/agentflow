package com.nakka.agentflow.agent;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nakka.agentflow.api.dto.AgentRunRequest;
import com.nakka.agentflow.api.dto.AgentRunResponse;
import com.nakka.agentflow.llm.BedrockClient;

class BedrockAgentServiceTest {

    private final BedrockClient bedrockClient = mock(BedrockClient.class);
    private final BedrockAgentService service = new BedrockAgentService(bedrockClient);

    @Test
    void returnsResponseFromBedrockClient() {
        when(bedrockClient.generate(anyString()))
                .thenReturn("Spring Boot health checks are configured via Actuator...");

        AgentRunRequest request = new AgentRunRequest("How do I configure Spring Boot health checks?", null);

        AgentRunResponse response = service.run(request);

        assertThat(response.response()).contains("Actuator");
        verify(bedrockClient).generate("How do I configure Spring Boot health checks?");
    }

    @Test
    void preservesProvidedConversationId() {
        when(bedrockClient.generate(anyString())).thenReturn("any response");

        AgentRunRequest request = new AgentRunRequest("query", "conv-99");

        AgentRunResponse response = service.run(request);

        assertThat(response.conversationId()).isEqualTo("conv-99");
    }

    @Test
    void measuresLatency() {
        when(bedrockClient.generate(anyString())).thenAnswer(invocation -> {
            Thread.sleep(10);
            return "response";
        });

        AgentRunRequest request = new AgentRunRequest("query", null);

        AgentRunResponse response = service.run(request);

        assertThat(response.latencyMs()).isGreaterThanOrEqualTo(10);
    }
}