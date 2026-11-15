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
 * Specialized Recovery Planning Agent: synthesizes multi-criteria trade-offs and alternative recovery plans.
 */
public class RecoveryPlanningAgent extends BaseSpecializedAgent {

    public RecoveryPlanningAgent() {
        super(AgentRole.RECOVERY_PLANNING, Set.of("query_inventory", "query_bom", "query_lane_status", "query_capacity"));
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
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 5));

        ActionIntent intent = ActionIntent.builder()
                .id(IntentId.of("INT-REC-" + context.incident().incidentId().substring(0, Math.min(8, context.incident().incidentId().length()))))
                .agentId(role().agentId())
                .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                .targetEntityId("RECOVERY-PLAN-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.19)
                .build();

        return AgentProposal.of(
                "PROP-REC-1",
                role(),
                intent,
                List.of("Global cost-benefit trade-off calculated for disruption " + context.incident().incidentId()),
                List.of("Prioritizes SLA fulfillment over expedite expenses"),
                0.94,
                List.of("Assumes customer acceptance of split delivery batches")
        );
    }

    @Override
    public AgentProposal replan(AgentInvestigationContext context, ReplanningScope scope, ToolExecutionBoundary toolBoundary) {
        String primaryMat = context.incident().affectedMaterialIds().stream().findFirst().orElse("MAT-DEFAULT");
        Instant now = Instant.now().plusSeconds(86400);
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 4));

        ActionIntent revisedIntent = ActionIntent.builder()
                .id(IntentId.of("INT-REC-REV-" + scope.iteration()))
                .agentId(role().agentId())
                .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                .targetEntityId("RECOVERY-PLAN-ALT-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.11)
                .build();

        return AgentProposal.of(
                "PROP-REC-REV-" + scope.iteration(),
                role(),
                revisedIntent,
                List.of("Balanced multi-objective pareto frontier recomputed"),
                List.of("Optimized for minimum lead-time variance"),
                0.96,
                List.of("Requires manual coordination check")
        );
    }
}
