package com.nakka.agentflow.tools;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.nakka.agentflow.config.RetrievalProperties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DocSearchTool implements Tool {

    private final RestClient retrievalClient;
    private final RetrievalProperties properties;

    public DocSearchTool(@Qualifier("retrievalRestClient") RestClient retrievalClient,
                         RetrievalProperties properties) {
        this.retrievalClient = retrievalClient;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "doc_search";
    }

    @Override
    public String description() {
        return "Searches the engineering documentation corpus and returns the top-K most relevant chunks. "
                + "Use this when a question requires authoritative information from documentation. "
                + "Arguments: query (string, required), k (integer, optional, defaults to "
                + properties.defaultK() + ").";
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String query = (String) arguments.get("query");
        if (query == null || query.isBlank()) {
            return ToolResult.failure("Missing required argument: query");
        }

        int k = arguments.containsKey("k")
                ? ((Number) arguments.get("k")).intValue()
                : properties.defaultK();

        try {
            SearchResponse response = retrievalClient.post()
                    .uri("/search")
                    .body(new SearchRequest(query, k))
                    .retrieve()
                    .body(SearchResponse.class);

            if (response == null || response.hits() == null || response.hits().isEmpty()) {
                return ToolResult.success("No matching documentation found.", List.of());
            }

            String summary = String.format("Retrieved %d chunks (top score %.3f)",
                    response.hits().size(),
                    response.hits().get(0).score());

            log.info("DocSearch query='{}' k={} hits={} topScore={}",
                    query, k, response.hits().size(), response.hits().get(0).score());

            return ToolResult.success(summary, response.hits());
        } catch (RestClientException e) {
            log.error("DocSearch HTTP error: {}", e.getMessage());
            return ToolResult.failure("Retrieval service unavailable: " + e.getMessage());
        }
    }

    public record SearchRequest(String query, int k) {}

    public record SearchResponse(String query, List<SearchHit> hits) {}

    public record SearchHit(String id, double score, String source, String section, String text) {}
}