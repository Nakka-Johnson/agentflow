package com.nakka.agentflow.tools;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CodeReviewAssistTool implements Tool {

    private final DocSearchTool docSearchTool;

    @Override
    public String name() {
        return "code_review_assist";
    }

    @Override
    public String description() {
        return "Reviews a code diff against engineering style guidelines. "
                + "Retrieves relevant guidelines via doc_search and produces structured review comments. "
                + "Arguments: diff (string, required), styleHint (string, optional, e.g. 'Java exception handling').";
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String diff = (String) arguments.get("diff");
        if (diff == null || diff.isBlank()) {
            return ToolResult.failure("Missing required argument: diff");
        }

        String styleHint = (String) arguments.getOrDefault("styleHint", "general code style");

        ToolResult retrieval = docSearchTool.execute(Map.of(
                "query", "code review guidelines for " + styleHint,
                "k", 3
        ));

        if (!retrieval.success()) {
            return ToolResult.failure("Could not retrieve guidelines: " + retrieval.summary());
        }

        // TODO: replace with real Bedrock call once available.
        // Claude will receive: diff, retrieved guidelines, and produce structured review.
        List<ReviewComment> stubReview = List.of(
                new ReviewComment("placeholder.java", 1, "info",
                        "Stub review. Bedrock not yet wired; retrieval succeeded with hint '" + styleHint + "'.")
        );

        log.info("CodeReview executed: diffLen={} styleHint='{}' guidelinesRetrieved={}",
                diff.length(), styleHint, retrieval.summary());

        return ToolResult.success(
                "Stub review produced (1 comment). Retrieval: " + retrieval.summary(),
                stubReview);
    }

    public record ReviewComment(String file, int line, String severity, String comment) {}
}