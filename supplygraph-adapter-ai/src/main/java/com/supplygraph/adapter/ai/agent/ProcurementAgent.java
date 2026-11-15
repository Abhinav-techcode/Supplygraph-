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
 * Specialized Procurement Agent: secures supplier capacity, spots buys, and expedites purchase orders.
 */
public class ProcurementAgent extends BaseSpecializedAgent {

    public ProcurementAgent() {
        super(AgentRole.PROCUREMENT, Set.of("query_inventory", "query_bom"));
    }

    @Override
    public AgentProposal investigate(AgentInvestigationContext context, ToolExecutionBoundary toolBoundary) {
        String primaryMat = context.incident().affectedMaterialIds().stream().findFirst().orElse("MAT-DEFAULT");

        toolBoundary.execute(new com.supplygraph.application.tool.ToolExecutionRequest(
                "query_bom",
                identity(),
                context.incident().workspaceId(),
                Map.of("materialId", primaryMat)
        ));

        Instant now = Instant.now();
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 6));

        ResourceClaim claim = ResourceClaim.consumptive(
                ResourceIdentity.of("SUPPLIER_CATALOG", "SUPPLIER-PO-" + primaryMat, "SUPPLIER-1"),
                Quantity.of(1000, UnitOfMeasure.PIECE),
                window
        );

        ActionIntent intent = ActionIntent.builder()
                .id(IntentId.of("INT-PROC-" + context.incident().incidentId().substring(0, Math.min(8, context.incident().incidentId().length()))))
                .agentId(role().agentId())
                .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                .targetEntityId("SUPPLIER-PO-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .quantity(Quantity.of(1000, UnitOfMeasure.PIECE))
                .addResourceClaim(claim)
                .riskScore(0.30)
                .build();

        return AgentProposal.of(
                "PROP-PROC-1",
                role(),
                intent,
                List.of("BOM explosion verified required precursors for " + primaryMat, "Supplier primary quota allocated"),
                List.of("Tier-1 vendor confirms 4-day expedite manufacturing run"),
                0.89,
                List.of("15% expedited order surcharge")
        );
    }

    @Override
    public AgentProposal replan(AgentInvestigationContext context, ReplanningScope scope, ToolExecutionBoundary toolBoundary) {
        String primaryMat = context.incident().affectedMaterialIds().stream().findFirst().orElse("MAT-DEFAULT");
        Instant now = Instant.now().plusSeconds(86400 * 2);
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 5));

        ResourceClaim claim = ResourceClaim.consumptive(
                ResourceIdentity.of("SUPPLIER_CATALOG", "SUPPLIER-ALT-" + primaryMat, "SUPPLIER-2"),
                Quantity.of(800, UnitOfMeasure.PIECE),
                window
        );

        ActionIntent revisedIntent = ActionIntent.builder()
                .id(IntentId.of("INT-PROC-REV-" + scope.iteration()))
                .agentId(role().agentId())
                .actionType(ActionType.EXPEDITE_PURCHASE_ORDER)
                .targetEntityId("SUPPLIER-ALT-" + primaryMat)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .quantity(Quantity.of(800, UnitOfMeasure.PIECE))
                .addResourceClaim(claim)
                .riskScore(0.18)
                .build();

        return AgentProposal.of(
                "PROP-PROC-REV-" + scope.iteration(),
                role(),
                revisedIntent,
                List.of("Switched to secondary pre-qualified supplier catalog"),
                List.of("Supplier SLA guarantees delivery within adjusted window"),
                0.91,
                List.of("Standard pricing but 2 days slower lead time")
        );
    }
}
