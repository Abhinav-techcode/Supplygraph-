package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.AgentIdentity;
import com.supplygraph.application.agent.AgentRole;
import com.supplygraph.application.agent.DisruptionIncident;
import com.supplygraph.application.agent.SpecializedAgent;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionRequest;
import com.supplygraph.application.tool.ToolExecutionResult;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Base implementation providing identity management and secure tool execution wrapping
 * for specialized bounded agents.
 */
public abstract class BaseSpecializedAgent implements SpecializedAgent {

    private final AgentIdentity identity;

    protected BaseSpecializedAgent(AgentRole role, Set<String> allowedTools) {
        Objects.requireNonNull(role, "role must not be null");
        this.identity = AgentIdentity.of(role, allowedTools);
    }

    @Override
    public AgentRole role() {
        return identity.role();
    }

    @Override
    public AgentIdentity identity() {
        return identity;
    }

    protected ToolExecutionResult callTool(
            ToolExecutionBoundary boundary,
            DisruptionIncident incident,
            String toolName,
            Map<String, Object> parameters
    ) {
        return boundary.execute(new ToolExecutionRequest(
                toolName,
                identity,
                incident.workspaceId(),
                parameters
        ));
    }
}
