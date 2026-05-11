package com.nakka.agentflow.agent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nakka.agentflow.api.dto.AgentRunRequest;
import com.nakka.agentflow.api.dto.AgentRunResponse;

class StubAgentServiceTest {

    private final Orchestrator orchestrator = mock(Orchestrator.class);
    private final StubAgentService service = new StubAgentService(orchestrator);

    @Test
    void delegatesQueryToOrchestrator() {
        when(orchestrator.run(anyString())).thenReturn(
                new Orchestrator.OrchestratorResult(
                        "orchestrator response",
                        List.of(new Orchestrator.ToolInvocation("doc_search", "Retrieved 4 chunks", true))
                ));

        AgentRunResponse response = service.run(new AgentRunRequest("any query", null));

        assertThat(response.response()).isEqualTo("orchestrator response");
        assertThat(response.toolCalls()).hasSize(1);
        assertThat(response.toolCalls().get(0).toolName()).isEqualTo("doc_search");
    }

    @Test
    void preservesProvidedConversationId() {
        when(orchestrator.run(anyString())).thenReturn(
                new Orchestrator.OrchestratorResult("ok", List.of()));

        AgentRunResponse response = service.run(new AgentRunRequest("query", "conv-99"));

        assertThat(response.conversationId()).isEqualTo("conv-99");
    }

    @Test
    void generatesConversationIdWhenNoneProvided() {
        when(orchestrator.run(anyString())).thenReturn(
                new Orchestrator.OrchestratorResult("ok", List.of()));

        AgentRunResponse response = service.run(new AgentRunRequest("query", null));

        assertThat(response.conversationId()).isNotBlank();
    }
}