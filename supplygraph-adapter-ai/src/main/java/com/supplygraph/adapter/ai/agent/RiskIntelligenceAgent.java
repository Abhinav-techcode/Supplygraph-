package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.*;

import java.util.*;

/** Quantifies how far the disruption reaches and where the network has single points of failure. Advisory (no action). */
public class RiskIntelligenceAgent extends InsightAgent {
    public RiskIntelligenceAgent() { super(AgentRole.RISK_INTELLIGENCE, Set.of("query_lane_status", "query_inventory")); }

    @Override
    protected Draft analyse(TwinInsights ins, AgentInvestigationContext ctx, ToolExecutionBoundary b, int rank) {
        List<String> ev = new ArrayList<>();
        int facts = 0;
        String focus = ins.hitNodes().stream().map(NodeId::value).findFirst().orElse("GLOBAL");
        ev.add("Severity reported by the incident: " + ctx.incident().severity() + "; declared outage " + ctx.incident().estimatedDowntimeHours() + " h.");
        if (ins.graph().getNodeCount() > 0) { facts++; ev.add("Share of the network disrupted or downstream of the disruption: " + pct(ins.networkDisruptedShare()) + " of " + ins.graph().getNodeCount() + " facilities."); }
        if (ins.hasAffectedNodes()) {
            facts++;
            ToolExecutionResult lanes = tool(b, ctx, "query_lane_status", Map.of("originNodeId", focus));
            int blocked = ins.blockedLanes().size();
            ev.add(blocked + " outbound lane(s) of the disrupted facilities stop carrying freight" + (lanes != null ? " (lane tool confirmed " + lanes.data().get("matchingLanesCount") + " lane(s) at " + focus + ")." : "."));
            if (blocked > 0) facts++;
        }
        List<NodeId> spof = ins.singlePointsOfFailure();
        ev.add(spof.isEmpty() ? "No downstream facility depends exclusively on the disrupted zone." : "Single points of failure (all inbound lanes come from the disrupted zone): " + spof.stream().map(NodeId::value).toList() + ".");
        if (!ins.downstreamNodes().isEmpty()) facts++;
        return Draft.advisory(ev, List.of("A facility is a single point of failure when every inbound lane originates inside the disrupted/downstream zone."), List.of("Advisory: proposes no action itself."), facts / 4.0, focus);
    }
}
