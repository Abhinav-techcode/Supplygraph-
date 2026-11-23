package com.supplygraph.application.agent;

import java.time.Instant;

/**
 * Audited record of an agent investigation conducted under zero-trust tool execution boundaries.
 */
public record AgentInvestigationRecord(
        String investigationId,
        String workspaceId,
        String disruptionId,
        String agentName,
        String role,
        String targetEntity,
        int toolsInvoked,
        int intentsGenerated,
        double confidenceScore,
        String outcome,
        Instant timestamp,
        String summary
) {
    public AgentInvestigationRecord(String investigationId, String workspaceId, String disruptionId, String agentName, String role, String targetEntity,
                                    int toolsInvoked, int intentsGenerated, double confidenceScore, String outcome, Instant timestamp) {
        this(investigationId, workspaceId, disruptionId, agentName, role, targetEntity, toolsInvoked, intentsGenerated, confidenceScore, outcome, timestamp, "");
    }
}

