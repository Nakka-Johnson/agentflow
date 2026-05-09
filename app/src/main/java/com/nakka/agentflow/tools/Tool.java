package com.nakka.agentflow.tools;

import java.util.Map;

public interface Tool {

    String name();

    String description();

    ToolResult execute(Map<String, Object> arguments);

    record ToolResult(boolean success, String summary, Object data) {

        public static ToolResult success(String summary, Object data) {
            return new ToolResult(true, summary, data);
        }

        public static ToolResult failure(String summary) {
            return new ToolResult(false, summary, null);
        }
    }
}