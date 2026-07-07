package com.nakka.agentflow.agent;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "agentflow.agent.implementation", havingValue = "stub", matchIfMissing = true)
public class NaivePlanner implements Planner {

    @Override
    public Orchestrator.ToolPlan plan(String query) {
        String lower = query.toLowerCase();

        if (lower.contains("review") || lower.contains("diff") || lower.contains("style")) {
            log.debug("Naive plan: code_review_assist (matched review/diff/style)");
            return new Orchestrator.ToolPlan(
                    "code_review_assist",
                    "query mentions review/diff/style",
                    Map.of("diff", query, "styleHint", "general code style")
            );
        }

        log.debug("Naive plan: doc_search (default)");
        return new Orchestrator.ToolPlan(
                "doc_search",
                "default to documentation retrieval",
                Map.of("query", query)
        );
    }
}   