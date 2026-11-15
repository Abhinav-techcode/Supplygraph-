package com.supplygraph.application.tool;

import com.supplygraph.application.agent.AgentIdentity;
import com.supplygraph.domain.model.WorkspaceId;

import java.util.Map;
import java.util.Objects;

/**
 * Invocation payload supplied to a tool execution request.
 */
public record ToolExecutionRequest(
        String toolName,
        AgentIdentity agentIdentity,
        WorkspaceId workspaceId,
        Map<String, Object> parameters
) {
    public ToolExecutionRequest {
        Objects.requireNonNull(toolName, "toolName must not be null");
        Objects.requireNonNull(agentIdentity, "agentIdentity must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
    }
}
