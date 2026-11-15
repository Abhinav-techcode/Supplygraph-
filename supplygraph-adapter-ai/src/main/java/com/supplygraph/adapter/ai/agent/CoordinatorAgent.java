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
 * Specialized Coordinator Agent: orchestrates multi-agent boundaries and synthesizes the recovery framework.
 */
public class CoordinatorAgent extends BaseSpecializedAgent {

    public CoordinatorAgent() {
        super(AgentRole.COORDINATOR, Set.of("query_inventory", "query_bom", "query_lane_status", "query_capacity"));
    }

    @Override
    public AgentProposal investigate(AgentInvestigationContext context, ToolExecutionBoundary toolBoundary) {
        String primaryMat = context.incident().affectedMaterialIds().stream().findFirst().orElse("MAT-DEFAULT");
        
        // Execute tool call to check inventory context
        toolBoundary.execute(new com.supplygraph.application.tool.ToolExecutionRequest(
                "query_inventory",
                identity(),
                context.incident().workspaceId(),
                Map.of("materialId", primaryMat)
        ));

        Instant now = Instant.now();
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 3));

        ActionIntent intent = ActionIntent.builder()
                .id(IntentId.of("INT-COORD-" + context.incident().incidentId().substring(0, Math.min(8, context.incident().incidentId().length()))))
                .agentId(role().agentId())
                .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                .targetEntityId("PO-EXP-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.15)
                .build();

        return AgentProposal.of(
                "PROP-COORD-1",
                role(),
                intent,
                List.of("Disruption incident scope analyzed: " + context.incident().title(), "Evaluated primary affected material " + primaryMat),
                List.of("Supplier has expedited freight option available"),
                0.95,
                List.of("Subject to carrier transit window availability")
        );
    }

    @Override
    public AgentProposal replan(AgentInvestigationContext context, ReplanningScope scope, ToolExecutionBoundary toolBoundary) {
        String primaryMat = context.incident().affectedMaterialIds().stream().findFirst().orElse("MAT-DEFAULT");
        Instant now = Instant.now().plusSeconds(86400); // Shift by 1 day
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 3));

        ActionIntent revisedIntent = ActionIntent.builder()
                .id(IntentId.of("INT-COORD-REV-" + scope.iteration()))
                .agentId(role().agentId())
                .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                .targetEntityId("PO-EXP-ALT-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.10)
                .build();

        return AgentProposal.of(
                "PROP-COORD-REV-" + scope.iteration(),
                role(),
                revisedIntent,
                List.of("Replanned after scope invalidation in iteration " + scope.iteration()),
                List.of("Alternative PO delivery date accepted"),
                0.90,
                List.of("Higher expedite surcharge")
        );
    }
}
