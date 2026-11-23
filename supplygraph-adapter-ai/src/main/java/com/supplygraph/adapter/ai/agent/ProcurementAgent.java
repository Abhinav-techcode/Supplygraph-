package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.*;

import java.util.*;

/** Finds alternative suppliers outside the disruption zone that can still reach the exposed site, ranked by transit time. */
public class ProcurementAgent extends InsightAgent {
    public ProcurementAgent() { super(AgentRole.PROCUREMENT, Set.of("query_inventory", "query_bom")); }

    @Override
    protected Draft analyse(TwinInsights ins, AgentInvestigationContext ctx, ToolExecutionBoundary b, int rank) {
        List<String> ev = new ArrayList<>();
        Optional<NodeId> site = ins.exposedSite();
        Optional<MaterialId> mat = ins.material();
        String focus = site.map(NodeId::value).orElse("GLOBAL");
        List<Map.Entry<NodeId, TwinInsights.Route>> alts = ins.alternativeSuppliers();
        if (alts.isEmpty()) return Draft.advisory(List.of("No supplier outside the disruption zone has an undisrupted route to " + focus + "."), List.of(), List.of("Escalate to spot-market sourcing outside the modelled network."), site.isPresent() ? 0.35 : 0.0, focus);
        Map.Entry<NodeId, TwinInsights.Route> alt = alts.get(Math.min(rank, alts.size() - 1));
        double required = ins.requiredUnits();
        double facts = 2 + (required > 0 ? 1 : 0) + (mat.isPresent() ? 1 : 0);
        ev.add(alts.size() + " alternative supplier(s) can reach " + focus + " without crossing the disruption; best: " + alts.get(0).getKey().value() + " in " + n1(alts.get(0).getValue().hours()) + " h.");
        ev.add("Selected " + alt.getKey().value() + " via " + alt.getValue().text() + " (" + n1(alt.getValue().hours()) + " h, lane cost " + n1(alt.getValue().costPerKg()) + "/kg summed along the route).");
        mat.ifPresent(m -> { ToolExecutionResult t = tool(b, ctx, "query_inventory", Map.of("materialId", m.value(), "nodeId", alt.getKey().value())); ev.add(t != null ? "Supplier stock query answered by the inventory tool." : "Supplier stock could not be confirmed (tool unavailable)."); });
        if (required > 0) ev.add("Order quantity sized to the estimated requirement of " + n0(required) + " units.");
        return new Draft(ActionType.EXPEDITE_PURCHASE_ORDER, alt.getKey().value(), alt.getKey().value(), required, "NONE", null, null, ev,
                List.of("The supplier has capacity to ship the quantity; it is not yet confirmed (supplier collaboration module)."), List.of("Supplier capacity is unverified until the supplier confirms."),
                facts / 4.0, clamp(alt.getValue().hours() / 240.0) + 0.1, focus);
    }
}
