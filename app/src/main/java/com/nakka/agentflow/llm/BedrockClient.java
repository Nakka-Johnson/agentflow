package com.nakka.agentflow.llm;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nakka.agentflow.config.BedrockProperties;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agentflow.agent.implementation", havingValue = "bedrock")
public class BedrockClient {

    private final BedrockProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private BedrockRuntimeClient client;

    @PostConstruct
    void init() {
        this.client = BedrockRuntimeClient.builder()
                .region(Region.of(properties.region()))
                .build();
        log.info("Bedrock client initialized: region={}, model={}",
                properties.region(), properties.modelId());
    }

    @PreDestroy
    void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    public String generate(String userMessage) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "anthropic_version", "bedrock-2023-05-31",
                    "max_tokens", properties.maxTokens(),
                    "temperature", properties.temperature(),
                    "messages", new Object[]{
                            Map.of("role", "user", "content", userMessage)
                    }
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(properties.modelId())
                    .body(SdkBytes.fromUtf8String(requestJson))
                    .build();

            InvokeModelResponse response = client.invokeModel(request);
            String responseBody = response.body().asUtf8String();

            JsonNode root = objectMapper.readTree(responseBody);
            return root.path("content").path(0).path("text").asText();
        } catch (Exception e) {
            log.error("Bedrock invocation failed: {}", e.getMessage(), e);
            throw new BedrockException("Failed to generate response", e);
        }
    }
}