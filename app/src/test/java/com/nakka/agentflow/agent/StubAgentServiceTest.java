package com.nakka.agentflow.agent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nakka.agentflow.api.dto.AgentRunRequest;
import com.nakka.agentflow.api.dto.AgentRunResponse;
import com.nakka.agentflow.tools.DocSearchTool;
import com.nakka.agentflow.tools.Tool;

class StubAgentServiceTest {

    private final DocSearchTool docSearchTool = mock(DocSearchTool.class);
    private final StubAgentService service = new StubAgentService(docSearchTool);

    @Test
    void respondsWithStubMessageWhenRetrievalSucceeds() {
        when(docSearchTool.name()).thenReturn("doc_search");
        when(docSearchTool.execute(anyMap()))
                .thenReturn(Tool.ToolResult.success("Retrieved 4 chunks", List.of()));

        AgentRunResponse response = service.run(new AgentRunRequest("any query", null));

        assertThat(response.response()).contains("Retrieval found relevant context");
        assertThat(response.toolCalls()).hasSize(1);
        assertThat(response.toolCalls().get(0).toolName()).isEqualTo("doc_search");
    }

    @Test
    void reportsFailureWhenRetrievalFails() {
        when(docSearchTool.execute(anyMap()))
                .thenReturn(Tool.ToolResult.failure("service unreachable"));

        AgentRunResponse response = service.run(new AgentRunRequest("any query", null));

        assertThat(response.response()).contains("Retrieval failed");
        assertThat(response.toolCalls()).isEmpty();
    }

    @Test
    void preservesProvidedConversationId() {
        when(docSearchTool.execute(anyMap()))
                .thenReturn(Tool.ToolResult.success("ok", List.of()));

        AgentRunResponse response = service.run(new AgentRunRequest("query", "conv-99"));

        assertThat(response.conversationId()).isEqualTo("conv-99");
    }

    @Test
    void generatesConversationIdWhenNoneProvided() {
        when(docSearchTool.execute(anyMap()))
                .thenReturn(Tool.ToolResult.success("ok", List.of()));

        AgentRunResponse response = service.run(new AgentRunRequest("query", null));

        assertThat(response.conversationId()).isNotBlank();
    }
}