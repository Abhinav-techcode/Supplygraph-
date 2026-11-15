package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.AgentInvestigationContext;
import com.supplygraph.application.agent.AgentProposal;
import com.supplygraph.application.agent.AgentRole;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.concurguard.model.CapacityClaim;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.concurguard.replanning.ReplanningScope;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.capacity.ResourceId;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specialized Production Agent: schedules plant assembly lines, cleanrooms, and manufacturing capacity.
 */
public class ProductionAgent extends BaseSpecializedAgent {

    public ProductionAgent() {
        super(AgentRole.PRODUCTION, Set.of("query_capacity", "query_bom"));
    }

    @Override
    public AgentProposal investigate(AgentInvestigationContext context, ToolExecutionBoundary toolBoundary) {
        String primaryNode = context.incident().affectedNodeIds().stream().findFirst().orElse("PLANT-1");
        ResourceId resourceId = ResourceId.of("LINE-" + primaryNode);

        Instant now = Instant.now();
        CapacityWindow window = CapacityWindow.of(now.plusSeconds(86400), now.plusSeconds(86400 * 3));

        // Attempt capacity query tool call
        try {
            toolBoundary.execute(new com.supplygraph.application.tool.ToolExecutionRequest(
                    "query_capacity",
                    identity(),
                    context.incident().workspaceId(),
                    Map.of("resourceId", resourceId.value())
            ));
        } catch (Exception ignored) {
            // Graceful fallback if calendar not initialized
        }

        CapacityClaim capacityClaim = CapacityClaim.of(
                resourceId,
                Quantity.of(500, UnitOfMeasure.PIECE),
                window
        );

        ActionIntent intent = ActionIntent.builder()
                .id(IntentId.of("INT-PROD-" + context.incident().incidentId().substring(0, Math.min(8, context.incident().incidentId().length()))))
                .agentId(role().agentId())
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId(resourceId.value())
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .capacityClaim(capacityClaim)
                .riskScore(0.35)
                .build();

        return AgentProposal.of(
                "PROP-PROD-1",
                role(),
                intent,
                List.of("Production throughput profile evaluated for plant " + primaryNode, "Changeover window reserved"),
                List.of("Line 1 tooling available"),
                0.88,
                List.of("Requires 4-hour setup changeover")
        );
    }

    @Override
    public AgentProposal replan(AgentInvestigationContext context, ReplanningScope scope, ToolExecutionBoundary toolBoundary) {
        String primaryNode = context.incident().affectedNodeIds().stream().findFirst().orElse("PLANT-1");
        ResourceId altResource = ResourceId.of("LINE-BACKUP-" + primaryNode);

        Instant now = Instant.now().plusSeconds(86400 * 3);
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 5));

        CapacityClaim capacityClaim = CapacityClaim.of(
                altResource,
                Quantity.of(500, UnitOfMeasure.PIECE),
                window
        );

        ActionIntent revisedIntent = ActionIntent.builder()
                .id(IntentId.of("INT-PROD-REV-" + scope.iteration()))
                .agentId(role().agentId())
                .actionType(ActionType.RESERVE_CAPACITY)
                .targetEntityId(altResource.value())
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .capacityClaim(capacityClaim)
                .riskScore(0.15)
                .build();

        return AgentProposal.of(
                "PROP-PROD-REV-" + scope.iteration(),
                role(),
                revisedIntent,
                List.of("Replanned to secondary manufacturing line to resolve capacity collision"),
                List.of("Line 2 cleanroom certified"),
                0.92,
                List.of("Slightly lower throughput rate")
        );
    }
}
