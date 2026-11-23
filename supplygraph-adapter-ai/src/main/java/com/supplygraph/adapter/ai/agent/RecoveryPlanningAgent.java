package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.*;

import java.util.*;

/** Judges whether available stock covers the requirement and reserves capacity for any shortfall. */
public class RecoveryPlanningAgent extends InsightAgent {
    public RecoveryPlanningAgent() { super(AgentRole.RECOVERY_PLANNING, Set.of("query_inventory", "query_bom", "query_lane_status", "query_capacity")); }

    @Override
    protected Draft analyse(TwinInsights ins, AgentInvestigationContext ctx, ToolExecutionBoundary b, int rank) {
        List<String> ev = new ArrayList<>();
        Optional<MaterialId> mat = ins.material();
        Optional<NodeId> site = ins.exposedSite();
        String focus = site.map(NodeId::value).orElse("GLOBAL");
        double required = ins.requiredUnits();
        if (mat.isEmpty() || site.isEmpty() || required <= 0) return Draft.advisory(List.of("Requirement could not be estimated from the twin (needs a downstream site with a known throughput and a material)."), List.of(), List.of(), 0.2, focus);
        double usable = ins.stockHolders(mat.get()).stream().filter(h -> !h.node().equals(site.get()) && ins.fastestRoute(h.node(), site.get()).isPresent()).mapToDouble(TwinInsights.StockHolder::onHand).sum();
        int suppliers = ins.alternativeSuppliers().size();
        double coverage = Math.min(1.0, usable / required);
        ev.add("Stock reachable over undisrupted routes: " + n0(usable) + " units against a requirement of " + n0(required) + " (" + pct(coverage) + " coverage).");
        ev.add(suppliers + " alternative supplier(s) can reach " + site.get().value() + ".");
        double shortfall = Math.max(0, required - usable);
        if (shortfall <= 0) { ev.add("Buffer stock alone covers the outage; no extra capacity reservation needed."); return Draft.advisory(ev, List.of("Requirement estimate holds."), List.of("Advisory: proposes no action."), 0.9, focus); }
        ev.add("Shortfall of " + n0(shortfall) + " units must come from alternative sourcing or production re-sequencing; reserving capacity at " + site.get().value() + " for the gap.");
        return new Draft(ActionType.RESERVE_CAPACITY, site.get().value(), site.get().value(), shortfall, "NONE", null, null, ev,
                List.of("Shortfall is covered by the other agents' actions or by reserved capacity."), List.of("The reservation is a placeholder for the shortfall; it does not source material."),
                0.9, clamp(shortfall / required), focus);
    }
}
