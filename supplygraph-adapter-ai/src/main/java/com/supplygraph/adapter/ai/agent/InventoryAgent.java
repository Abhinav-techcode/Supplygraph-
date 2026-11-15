package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.AgentInvestigationContext;
import com.supplygraph.application.agent.AgentProposal;
import com.supplygraph.application.agent.AgentRole;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.concurguard.model.*;
import com.supplygraph.concurguard.replanning.ReplanningScope;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Specialized Inventory Agent: monitors safety stock buffers and proposes rebalancing allocations.
 */
public class InventoryAgent extends BaseSpecializedAgent {

    public InventoryAgent() {
        super(AgentRole.INVENTORY, Set.of("query_inventory", "query_bom"));
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
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 4));

        ResourceClaim claim = ResourceClaim.consumptive(
                ResourceIdentity.of("INVENTORY_ACCOUNT", "STOCK-" + primaryMat, "FACILITY-1"),
                Quantity.of(500, UnitOfMeasure.PIECE),
                window
        );

        ActionIntent intent = ActionIntent.builder()
                .id(IntentId.of("INT-INV-" + context.incident().incidentId().substring(0, Math.min(8, context.incident().incidentId().length()))))
                .agentId(role().agentId())
                .actionType(ActionType.REALLOCATE_INVENTORY)
                .targetEntityId("STOCK-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .quantity(Quantity.of(500, UnitOfMeasure.PIECE))
                .addResourceClaim(claim)
                .riskScore(0.25)
                .build();

        return AgentProposal.of(
                "PROP-INV-1",
                role(),
                intent,
                List.of("Safety stock buffer examined for material: " + primaryMat, "Identified available buffer balance"),
                List.of("Minimum safety stock threshold maintained at 20%"),
                0.90,
                List.of("Reduces regional buffer for 48 hours")
        );
    }

    @Override
    public AgentProposal replan(AgentInvestigationContext context, ReplanningScope scope, ToolExecutionBoundary toolBoundary) {
        String primaryMat = context.incident().affectedMaterialIds().stream().findFirst().orElse("MAT-DEFAULT");
        Instant now = Instant.now().plusSeconds(86400);
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 4));

        ResourceClaim claim = ResourceClaim.consumptive(
                ResourceIdentity.of("INVENTORY_ACCOUNT", "STOCK-" + primaryMat, "FACILITY-1"),
                Quantity.of(250, UnitOfMeasure.PIECE), // Reduced claim to avoid overcommit
                window
        );

        ActionIntent revisedIntent = ActionIntent.builder()
                .id(IntentId.of("INT-INV-REV-" + scope.iteration()))
                .agentId(role().agentId())
                .actionType(ActionType.REALLOCATE_INVENTORY)
                .targetEntityId("STOCK-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .quantity(Quantity.of(250, UnitOfMeasure.PIECE))
                .addResourceClaim(claim)
                .riskScore(0.15)
                .build();

        return AgentProposal.of(
                "PROP-INV-REV-" + scope.iteration(),
                role(),
                revisedIntent,
                List.of("Replanned inventory drawdown with reduced claim of 250 units"),
                List.of("Secondary hub provides replenishment"),
                0.93,
                List.of("Requires cross-dock staging")
        );
    }
}
