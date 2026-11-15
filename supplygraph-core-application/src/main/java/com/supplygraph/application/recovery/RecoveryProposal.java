package com.supplygraph.application.recovery;

import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.domain.model.WorkspaceId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable recovery proposal produced by the agentic recovery pipeline.
 * Stored for human-in-the-loop decision-maker review; never auto-executed.
 */
public record RecoveryProposal(
        String proposalId,
        String incidentId,
        WorkspaceId workspaceId,
        List<ActionIntent> approvedIntents,
        List<ActionIntent> rejectedIntents,
        List<ActionIntent> preservedIntents,
        List<String> arbitratedTradeOffs,
        BigDecimal totalEstimatedCost,
        long recoveryTimeObjectiveHours,
        double overallConfidence,
        String riskAssessmentSummary,
        List<ToolExecutionBoundary.ToolAuditEntry> auditTrail,
        Instant createdAt
) {
    public RecoveryProposal {
        Objects.requireNonNull(proposalId, "proposalId must not be null");
        Objects.requireNonNull(incidentId, "incidentId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        approvedIntents = approvedIntents != null ? List.copyOf(approvedIntents) : List.of();
        rejectedIntents = rejectedIntents != null ? List.copyOf(rejectedIntents) : List.of();
        preservedIntents = preservedIntents != null ? List.copyOf(preservedIntents) : List.of();
        arbitratedTradeOffs = arbitratedTradeOffs != null ? List.copyOf(arbitratedTradeOffs) : List.of();
        Objects.requireNonNull(totalEstimatedCost, "totalEstimatedCost must not be null");
        auditTrail = auditTrail != null ? List.copyOf(auditTrail) : List.of();
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
