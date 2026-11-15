package com.supplygraph.application.agent;

import com.supplygraph.concurguard.model.AgentId;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable identity and security credentials of an agent operating within SupplyGraph.
 */
public record AgentIdentity(
        AgentId agentId,
        AgentRole role,
        String displayName,
        Set<String> allowedTools
) {
    public AgentIdentity {
        Objects.requireNonNull(agentId, "agentId must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        allowedTools = allowedTools != null ? Set.copyOf(allowedTools) : Set.of();
    }

    public static AgentIdentity of(AgentRole role, Set<String> allowedTools) {
        return new AgentIdentity(role.agentId(), role, role.displayName(), allowedTools);
    }

    public boolean isToolAllowed(String toolName) {
        return allowedTools.contains(toolName);
    }
}
