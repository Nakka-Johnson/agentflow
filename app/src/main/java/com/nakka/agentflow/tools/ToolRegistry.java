package com.nakka.agentflow.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final List<Tool> tools;
    private final Map<String, Tool> toolsByName = new HashMap<>();

    @PostConstruct
    void register() {
        for (Tool tool : tools) {
            if (toolsByName.containsKey(tool.name())) {
                throw new IllegalStateException(
                        "Duplicate tool name: " + tool.name());
            }
            toolsByName.put(tool.name(), tool);
            log.info("Registered tool: name={} description='{}'",
                    tool.name(),
                    tool.description().substring(0, Math.min(80, tool.description().length())) + "...");
        }
        log.info("ToolRegistry initialized with {} tools", toolsByName.size());
    }

    public Optional<Tool> find(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    public List<Tool> all() {
        return List.copyOf(toolsByName.values());
    }

    public List<ToolDescriptor> describe() {
        return toolsByName.values().stream()
                .map(t -> new ToolDescriptor(t.name(), t.description()))
                .toList();
    }

    public record ToolDescriptor(String name, String description) {}
}