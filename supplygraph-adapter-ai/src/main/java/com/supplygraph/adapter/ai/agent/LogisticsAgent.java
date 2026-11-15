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
 * Specialized Logistics Agent: orchestrates freight rerouting, carrier capacity, and lane transit schedules.
 */
public class LogisticsAgent extends BaseSpecializedAgent {

    public LogisticsAgent() {
        super(AgentRole.LOGISTICS, Set.of("query_lane_status"));
    }

    @Override
    public AgentProposal investigate(AgentInvestigationContext context, ToolExecutionBoundary toolBoundary) {
        String primaryNode = context.incident().affectedNodeIds().stream().findFirst().orElse("NODE-DEFAULT");

        toolBoundary.execute(new com.supplygraph.application.tool.ToolExecutionRequest(
                "query_lane_status",
                identity(),
                context.incident().workspaceId(),
                Map.of("originNodeId", primaryNode)
        ));

        Instant now = Instant.now();
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 3));

        ActionIntent intent = ActionIntent.builder()
                .id(IntentId.of("INT-LOG-" + context.incident().incidentId().substring(0, Math.min(8, context.incident().incidentId().length()))))
                .agentId(role().agentId())
                .actionType(ActionType.REROUTE_SHIPMENT)
                .targetEntityId("LANE-LOG-" + primaryNode)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.22)
                .build();

        return AgentProposal.of(
                "PROP-LOG-1",
                role(),
                intent,
                List.of("Transportation lane transit status evaluated for hub: " + primaryNode, "Carrier dispatch confirmed"),
                List.of("Intermodal rail-truck connection synchronized"),
                0.91,
                List.of("Transit subject to customs border clearance")
        );
    }

    @Override
    public AgentProposal replan(AgentInvestigationContext context, ReplanningScope scope, ToolExecutionBoundary toolBoundary) {
        String primaryNode = context.incident().affectedNodeIds().stream().findFirst().orElse("NODE-DEFAULT");
        Instant now = Instant.now().plusSeconds(86400);
        CapacityWindow window = CapacityWindow.of(now, now.plusSeconds(86400 * 4));

        ActionIntent revisedIntent = ActionIntent.builder()
                .id(IntentId.of("INT-LOG-REV-" + scope.iteration()))
                .agentId(role().agentId())
                .actionType(ActionType.REROUTE_SHIPMENT)
                .targetEntityId("LANE-LOG-EXP-" + primaryNode)
                .expectedStateVersion(context.snapshot().version())
                .timeWindow(window)
                .riskScore(0.14)
                .build();

        return AgentProposal.of(
                "PROP-LOG-REV-" + scope.iteration(),
                role(),
                revisedIntent,
                List.of("Replanned logistics dispatch using dedicated air charter corridor"),
                List.of("Air cargo bay reserved"),
                0.94,
                List.of("Higher transport cost per kg")
        );
    }
}
