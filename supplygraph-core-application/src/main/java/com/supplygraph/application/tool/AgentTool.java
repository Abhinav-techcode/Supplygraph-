package com.supplygraph.application.tool;

/**
 * Functional contract for a bounded, read-only inspection tool callable by agents.
 * Implementing tools MUST NOT mutate any authoritative domain state.
 */
public interface AgentTool {

    String name();

    String description();

    ToolExecutionResult execute(ToolExecutionRequest request);
}
