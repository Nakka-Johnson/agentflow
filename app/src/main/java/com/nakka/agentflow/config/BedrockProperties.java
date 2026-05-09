package com.nakka.agentflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "agentflow.bedrock")
public record BedrockProperties(
    String region,
    String modelId,
    int maxTokens,
    double temperature
) {}