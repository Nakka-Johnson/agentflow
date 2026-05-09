package com.nakka.agentflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentflow.retrieval")
public record RetrievalProperties(
        String baseUrl,
        int defaultK
) {}