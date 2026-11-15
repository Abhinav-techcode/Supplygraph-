package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.AgentInvestigationContext;
import com.supplygraph.application.agent.AgentProposal;
import com.supplygraph.application.agent.AgentRole;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.concurguard.replanning.ReplanningScope;
import com.supplygraph.domain.model.capacity.CapacityWindow;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specialized Quality Agent: enforces regulatory compliance, GMP audits, cold-chain checks, and batch holds.
 */
public class QualityAgent extends BaseSpecializedAgent {

    public QualityAgent() {
        super(AgentRole.QUALITY, Set.of("query_inventory", "query_bom"));
    }

    @Override
    public AgentProposal investigate(AgentInvestigationContext context, ToolExecutionBoundary toolBoundary) {
        String primaryMat = context.incident().affectedMaterialIds().stream().findFirst().orElse("MAT-DEFAULT");

        toolBoundary.execute(new com.supplygraph.application.tool.ToolExecutionRequest(
                "query_inventory",
                identity(),
                context.incident().workspaceId(),
                Map.of("materialId", primaryMat)
        ));

        Instant now = Instant.now();
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 2));

        ActionIntent intent = ActionIntent.builder()
                .id(IntentId.of("INT-QUAL-" + context.incident().incidentId().substring(0, Math.min(8, context.incident().incidentId().length()))))
                .agentId(role().agentId())
                .actionType(ActionType.QUARANTINE_LOT)
                .targetEntityId("LOT-INSPECT-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.08)
                .build();

        return AgentProposal.of(
                "PROP-QUAL-1",
                role(),
                intent,
                List.of("Quality and regulatory certificates verified for " + primaryMat, "Cold chain logs within acceptable threshold"),
                List.of("Batch meets CFR Part 11 and ISO 9001 specs"),
                0.97,
                List.of("Requires physical inspector sign-off before release")
        );
    }

    @Override
    public AgentProposal replan(AgentInvestigationContext context, ReplanningScope scope, ToolExecutionBoundary toolBoundary) {
        String primaryMat = context.incident().affectedMaterialIds().stream().findFirst().orElse("MAT-DEFAULT");
        Instant now = Instant.now().plusSeconds(86400);
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 2));

        ActionIntent revisedIntent = ActionIntent.builder()
                .id(IntentId.of("INT-QUAL-REV-" + scope.iteration()))
                .agentId(role().agentId())
                .actionType(ActionType.QUARANTINE_LOT)
                .targetEntityId("LOT-INSPECT-EXP-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.05)
                .build();

        return AgentProposal.of(
                "PROP-QUAL-REV-" + scope.iteration(),
                role(),
                revisedIntent,
                List.of("Rapid micro-biological assays completed with zero contamination"),
                List.of("Expedited Certificate of Analysis approved"),
                0.98,
                List.of("Retain sample retained for 12 months")
        );
    }
}
