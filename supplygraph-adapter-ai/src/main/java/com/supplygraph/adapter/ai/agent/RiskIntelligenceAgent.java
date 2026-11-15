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
 * Specialized Risk Intelligence Agent: detects external shocks, lead-time variances, and geopolitical risks.
 */
public class RiskIntelligenceAgent extends BaseSpecializedAgent {

    public RiskIntelligenceAgent() {
        super(AgentRole.RISK_INTELLIGENCE, Set.of("query_lane_status", "query_inventory"));
    }

    @Override
    public AgentProposal investigate(AgentInvestigationContext context, ToolExecutionBoundary toolBoundary) {
        String primaryNode = context.incident().affectedNodeIds().stream().findFirst().orElse("NODE-DEFAULT");

        // Execute lane query tool call
        toolBoundary.execute(new com.supplygraph.application.tool.ToolExecutionRequest(
                "query_lane_status",
                identity(),
                context.incident().workspaceId(),
                Map.of("originNodeId", primaryNode)
        ));

        Instant now = Instant.now();
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 5));

        ActionIntent intent = ActionIntent.builder()
                .id(IntentId.of("INT-RISK-" + context.incident().incidentId().substring(0, Math.min(8, context.incident().incidentId().length()))))
                .agentId(role().agentId())
                .actionType(ActionType.REROUTE_SHIPMENT)
                .targetEntityId("LANE-RISK-AVOID-" + primaryNode)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.20)
                .build();

        return AgentProposal.of(
                "PROP-RISK-1",
                role(),
                intent,
                List.of("Severe weather or disruption anomaly detected at origin " + primaryNode),
                List.of("Alternate transit lane operational"),
                0.92,
                List.of("Increased fuel surcharge by 8%")
        );
    }

    @Override
    public AgentProposal replan(AgentInvestigationContext context, ReplanningScope scope, ToolExecutionBoundary toolBoundary) {
        String primaryNode = context.incident().affectedNodeIds().stream().findFirst().orElse("NODE-DEFAULT");
        Instant now = Instant.now().plusSeconds(86400 * 2);
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 4));

        ActionIntent revisedIntent = ActionIntent.builder()
                .id(IntentId.of("INT-RISK-REV-" + scope.iteration()))
                .agentId(role().agentId())
                .actionType(ActionType.REROUTE_SHIPMENT)
                .targetEntityId("LANE-SEC-AIR-" + primaryNode)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.12)
                .build();

        return AgentProposal.of(
                "PROP-RISK-REV-" + scope.iteration(),
                role(),
                revisedIntent,
                List.of("Replanned risk mitigation via multimodal air routing"),
                List.of("Dedicated air charter capacity reserved"),
                0.88,
                List.of("Premium freight expense incurred")
        );
    }
}
