package com.nakka.agentflow.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.nakka.agentflow.tools.Tool;
import com.nakka.agentflow.tools.ToolRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class Orchestrator {

    private final ToolRegistry toolRegistry;

    public OrchestratorResult run(String query) {
        OrchestratorTrace trace = new OrchestratorTrace();
        AgentState state = AgentState.INIT;

        log.info("Orchestrator starting: query='{}'", query);

        while (state != AgentState.DONE) {
            state = transition(state, query, trace);
            trace.recordStateTransition(state);
        }

        log.info("Orchestrator done: stateTransitions={} toolInvocations={}",
                trace.stateTransitions().size(), trace.toolInvocations().size());

        return new OrchestratorResult(trace.finalResponse(), trace.toolInvocations());
    }

    private AgentState transition(AgentState current, String query, OrchestratorTrace trace) {
        return switch (current) {
            case INIT -> AgentState.PLAN;

            case PLAN -> {
                ToolPlan plan = planNaive(query);
                trace.recordPlan(plan);
                log.info("Planned tool: name={} reason='{}'", plan.toolName(), plan.reason());
                yield AgentState.ACT;
            }

            case ACT -> {
                ToolPlan plan = trace.lastPlan();
                Tool tool = toolRegistry.find(plan.toolName())
                        .orElseThrow(() -> new IllegalStateException(
                                "Planned tool not found in registry: " + plan.toolName()));

                Tool.ToolResult result = tool.execute(plan.arguments());
                trace.recordToolInvocation(plan.toolName(), result);

                yield AgentState.RESPOND;
            }

            case RESPOND -> {
                Tool.ToolResult lastResult = trace.lastToolResult();
                String response = lastResult.success()
                        ? "Tool " + trace.lastPlan().toolName() + " executed successfully. "
                                + "Summary: " + lastResult.summary() + ". "
                                + "(Bedrock will compose the final answer here once wired up.)"
                        : "Tool execution failed: " + lastResult.summary();
                trace.recordResponse(response);
                yield AgentState.DONE;
            }

            case DONE -> AgentState.DONE;
        };
    }

    /**
     * Naive planner. Today: keyword matching.
     * Tomorrow: replaced with Claude as the planner via Bedrock tool-use API.
     */
    private ToolPlan planNaive(String query) {
        String lower = query.toLowerCase();
        if (lower.contains("review") || lower.contains("diff") || lower.contains("style")) {
            return new ToolPlan(
                    "code_review_assist",
                    "query mentions review/diff/style",
                    Map.of("diff", query, "styleHint", "general code style")
            );
        }
        return new ToolPlan(
                "doc_search",
                "default to documentation retrieval",
                Map.of("query", query)
        );
    }

    public enum AgentState {
        INIT, PLAN, ACT, RESPOND, DONE
    }

    public record ToolPlan(String toolName, String reason, Map<String, Object> arguments) {}

    public record OrchestratorResult(String response, List<ToolInvocation> toolInvocations) {}

    public record ToolInvocation(String toolName, String summary, boolean success) {}

    /**
     * Mutable trace built up as the orchestrator runs.
     * Records every state transition, plan, and tool invocation for observability.
     */
    static class OrchestratorTrace {
        private final List<AgentState> stateTransitions = new ArrayList<>();
        private final List<ToolInvocation> toolInvocations = new ArrayList<>();
        private ToolPlan lastPlan;
        private Tool.ToolResult lastToolResult;
        private String finalResponse;

        void recordStateTransition(AgentState state) { stateTransitions.add(state); }
        void recordPlan(ToolPlan plan) { this.lastPlan = plan; }
        void recordToolInvocation(String toolName, Tool.ToolResult result) {
            this.lastToolResult = result;
            toolInvocations.add(new ToolInvocation(toolName, result.summary(), result.success()));
        }
        void recordResponse(String response) { this.finalResponse = response; }

        List<AgentState> stateTransitions() { return stateTransitions; }
        List<ToolInvocation> toolInvocations() { return toolInvocations; }
        ToolPlan lastPlan() { return lastPlan; }
        Tool.ToolResult lastToolResult() { return lastToolResult; }
        String finalResponse() { return finalResponse; }
    }
}