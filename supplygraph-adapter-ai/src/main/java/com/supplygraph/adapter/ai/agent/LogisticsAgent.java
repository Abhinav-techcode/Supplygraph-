package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.*;

import java.util.*;

/** Reroutes freight around blocked lanes: finds the fastest undisrupted route into the exposed site and claims its first lane. */
public class LogisticsAgent extends InsightAgent {
    public LogisticsAgent() { super(AgentRole.LOGISTICS, Set.of("query_lane_status")); }

    @Override
    protected Draft analyse(TwinInsights ins, AgentInvestigationContext ctx, ToolExecutionBoundary b, int rank) {
        List<String> ev = new ArrayList<>();
        Optional<NodeId> site = ins.exposedSite();
        String focus = ins.hitNodes().stream().map(NodeId::value).findFirst().orElse("GLOBAL");
        if (site.isEmpty()) return Draft.advisory(List.of("No downstream facility is exposed; nothing to reroute."), List.of(), List.of(), ins.hasAffectedNodes() ? 0.5 : 0.0, focus);
        ToolExecutionResult lanes = tool(b, ctx, "query_lane_status", Map.of("originNodeId", focus));
        int blocked = ins.blockedLanes().size();
        ev.add(blocked + " lane(s) leave the disrupted facilities" + (lanes != null ? " (lane tool confirmed)." : "."));
        Optional<MaterialId> mat = ins.material();
        List<NodeId> origins = new ArrayList<>();
        if (mat.isPresent()) ins.stockHolders(mat.get()).stream().map(TwinInsights.StockHolder::node).filter(n -> !n.equals(site.get())).forEach(origins::add);
        ins.alternativeSuppliers().stream().map(Map.Entry::getKey).filter(n -> !origins.contains(n)).forEach(origins::add);
        Optional<TwinInsights.Route> route = Optional.empty();
        NodeId origin = null;
        for (int i = 0; i < origins.size() && route.isEmpty(); i++) {
            origin = origins.get(Math.min(rank + i, origins.size() - 1));
            route = ins.fastestRoute(origin, site.get());
        }
        if (route.isEmpty()) return Draft.advisory(ev, List.of(), List.of("No undisrupted route reaches " + site.get().value() + "."), 0.4, focus);
        TwinInsights.Route r = route.get();
        NodeId first = r.nodes().get(0), second = r.nodes().get(1);
        String laneId = ins.graph().getOutgoingLanes(first).stream().filter(l -> l.getTargetNodeId().equals(second)).map(l -> l.getId().value()).findFirst().orElse(first.value() + "->" + second.value());
        ev.add("Undisrupted route from " + origin.value() + " to " + site.get().value() + ": " + r.text() + " in " + n1(r.hours()) + " h, lane cost " + n1(r.costPerKg()) + "/kg summed.");
        double qty = Math.max(0, ins.requiredUnits());
        return new Draft(ActionType.REROUTE_SHIPMENT, site.get().value(), origin.value(), qty, "LANE", laneId, first.value(), ev,
                List.of("Carrier capacity exists on the chosen lanes at nominal transit time."), List.of("Lane capacity is not modelled in the twin; the shared claim only detects double-booking between plans."),
                (1 + (lanes != null ? 1 : 0) + 1 + (qty > 0 ? 1 : 0)) / 4.0, clamp(r.hours() / 240.0) + 0.1, focus);
    }
}
