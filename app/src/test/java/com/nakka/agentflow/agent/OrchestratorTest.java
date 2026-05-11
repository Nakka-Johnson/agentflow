package com.nakka.agentflow.agent;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nakka.agentflow.tools.Tool;
import com.nakka.agentflow.tools.ToolRegistry;

class OrchestratorTest {

    private final ToolRegistry registry = mock(ToolRegistry.class);
    private final Tool docSearch = mock(Tool.class);
    private final Tool codeReview = mock(Tool.class);
    private final Orchestrator orchestrator = new Orchestrator(registry);

    @Test
    void routesGeneralQueryToDocSearch() {
        when(docSearch.name()).thenReturn("doc_search");
        when(docSearch.execute(anyMap()))
                .thenReturn(Tool.ToolResult.success("Retrieved 4 chunks", List.of()));
        when(registry.find("doc_search")).thenReturn(Optional.of(docSearch));

        Orchestrator.OrchestratorResult result = orchestrator.run("How do I configure logging?");

        assertThat(result.toolInvocations()).hasSize(1);
        assertThat(result.toolInvocations().get(0).toolName()).isEqualTo("doc_search");
        assertThat(result.toolInvocations().get(0).success()).isTrue();
        assertThat(result.response()).contains("doc_search");
    }

    @Test
    void routesReviewQueryToCodeReview() {
        when(codeReview.name()).thenReturn("code_review_assist");
        when(codeReview.execute(anyMap()))
                .thenReturn(Tool.ToolResult.success("Stub review (1 comment)", List.of()));
        when(registry.find("code_review_assist")).thenReturn(Optional.of(codeReview));

        Orchestrator.OrchestratorResult result = orchestrator.run("review this diff please");

        assertThat(result.toolInvocations()).hasSize(1);
        assertThat(result.toolInvocations().get(0).toolName()).isEqualTo("code_review_assist");
    }

    @Test
    void surfacesFailureWhenToolFails() {
        when(docSearch.name()).thenReturn("doc_search");
        when(docSearch.execute(anyMap()))
                .thenReturn(Tool.ToolResult.failure("retrieval service unreachable"));
        when(registry.find("doc_search")).thenReturn(Optional.of(docSearch));

        Orchestrator.OrchestratorResult result = orchestrator.run("anything");

        assertThat(result.toolInvocations().get(0).success()).isFalse();
        assertThat(result.response()).contains("failed");
    }

    @Test
    void throwsWhenPlannedToolNotInRegistry() {
        when(registry.find(any())).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> orchestrator.run("anything")
        );
    }
}