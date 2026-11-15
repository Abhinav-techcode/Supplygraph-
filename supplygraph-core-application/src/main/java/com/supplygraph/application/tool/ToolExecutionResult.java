package com.supplygraph.application.tool;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Result returned from a tool execution boundary call.
 */
public record ToolExecutionResult(
        boolean success,
        Map<String, Object> data,
        String errorMessage,
        Instant executedAt
) {
    public ToolExecutionResult {
        data = data != null ? Map.copyOf(data) : Map.of();
        Objects.requireNonNull(executedAt, "executedAt must not be null");
    }

    public static ToolExecutionResult success(Map<String, Object> data) {
        return new ToolExecutionResult(true, data, null, Instant.now());
    }

    public static ToolExecutionResult failure(String errorMessage) {
        return new ToolExecutionResult(false, Map.of(), errorMessage, Instant.now());
    }
}
