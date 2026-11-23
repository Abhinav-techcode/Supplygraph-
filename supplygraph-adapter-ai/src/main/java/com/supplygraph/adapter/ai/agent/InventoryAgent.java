package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.*;

import java.util.*;

/** Releases buffer stock from the best-placed holder outside the disruption zone, claiming that stock so conflicting plans are caught. */
public class InventoryAgent extends InsightAgent {
    public InventoryAgent() { super(AgentRole.INVENTORY, Set.of("query_inventory", "query_bom")); }

    @Override
    protected Draft analyse(TwinInsights ins, AgentInvestigationContext ctx, ToolExecutionBoundary b, int rank) {
        List<String> ev = new ArrayList<>();
        Optional<MaterialId> mat = ins.material();
        Optional<NodeId> site = ins.exposedSite();
        String focus = site.map(NodeId::value).orElse("GLOBAL");
        if (mat.isEmpty() || site.isEmpty()) return Draft.advisory(List.of("No material or exposed site could be derived from the twin; no buffer release proposed."), List.of(), List.of("Needs the material and an exposed downstream site."), 0.0, focus);
        ToolExecutionResult inv = tool(b, ctx, "query_inventory", Map.of("materialId", mat.get().value()));
        List<TwinInsights.StockHolder> holders = ins.stockHolders(mat.get()).stream().filter(h -> !h.node().equals(site.get())).toList();
        if (holders.isEmpty()) return Draft.advisory(List.of("No stock of " + ins.materialName(mat.get()) + " exists outside the disruption zone."), List.of(), List.of("Consider alternative sourcing."), 0.25, focus);
        TwinInsights.StockHolder h = holders.get(Math.min(rank, holders.size() - 1));
        Optional<TwinInsights.Route> route = ins.fastestRoute(h.node(), site.get());
        double required = ins.requiredUnits();
        double qty = required > 0 ? Math.min(required, h.onHand()) : 0;
        double coverage = required > 0 ? qty / required : 0;
        double facts = 1 + (route.isPresent() ? 1 : 0) + (required > 0 ? 1 : 0) + (inv != null ? 1 : 0);
        ev.add(h.node().value() + " holds " + n0(h.onHand()) + " units of " + ins.materialName(mat.get()) + (inv != null ? " (confirmed by inventory tool)." : "."));
        route.ifPresentOrElse(r -> ev.add("Fastest undisrupted route to " + site.get().value() + ": " + r.text() + " in " + n1(r.hours()) + " h."), () -> ev.add("No undisrupted route from " + h.node().value() + " to " + site.get().value() + "."));
        if (required > 0) ev.add("Release " + n0(qty) + " units: covers " + pct(coverage) + " of the estimated " + n0(required) + "-unit requirement.");
        if (rank > 0) ev.add("Replanning iteration " + rank + ": using the next-best stock holder because the first choice conflicted.");
        return new Draft(ActionType.EMERGENCY_BUFFER_DISPATCH, site.get().value(), h.node().value(), qty, "INVENTORY", mat.get().value(), h.node().value(), ev,
                List.of("Stock on hand is available for release (no other commitment recorded outside ConcurGuard)."), List.of(route.isEmpty() ? "No route found." : "Transit time is nominal."),
                facts / 4.0, clamp(1 - coverage) * 0.6 + (route.isEmpty() ? 0.3 : 0.05), focus);
    }
}
