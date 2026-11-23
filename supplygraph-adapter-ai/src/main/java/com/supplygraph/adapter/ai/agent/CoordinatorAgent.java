package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.*;

import java.util.*;

/** Synthesises the overall posture of the incident from the twin: what is hit, who is exposed, how long stock lasts. Advisory (no action). */
public class CoordinatorAgent extends InsightAgent {
    public CoordinatorAgent() { super(AgentRole.COORDINATOR, Set.of("query_inventory", "query_bom", "query_lane_status", "query_capacity")); }

    @Override
    protected Draft analyse(TwinInsights ins, AgentInvestigationContext ctx, ToolExecutionBoundary b, int rank) {
        List<String> ev = new ArrayList<>();
        int facts = 0;
        String focus = ins.hitNodes().stream().map(NodeId::value).findFirst().orElse("GLOBAL");
        if (ins.hasAffectedNodes()) { facts++; ev.add("Disrupted facilities in the twin: " + ins.hitNodes().stream().map(NodeId::value).toList() + "; " + ins.downstreamNodes().size() + " downstream facilities depend on them."); }
        else ev.add("None of the incident's affected facilities exist in the twin; no network posture can be derived.");
        Optional<NodeId> site = ins.exposedSite();
        if (site.isPresent()) { facts++; ev.add("Most exposed manufacturing/downstream site: " + site.get().value() + "."); }
        Optional<MaterialId> mat = ins.material();
        if (mat.isPresent()) {
            facts++;
            ToolExecutionResult inv = tool(b, ctx, "query_inventory", Map.of("materialId", mat.get().value()));
            ev.add("Material in focus: " + ins.materialName(mat.get()) + (inv != null ? " (inventory tool answered)." : " (inventory tool unavailable)."));
            if (ins.requiredUnits() > 0) { facts++; ev.add("Estimated requirement over the outage: " + n0(ins.requiredUnits()) + " units (throughput/h x 24 x outage days)."); }
            OptionalDouble tts = ins.timeToSurviveDays(mat.get());
            if (tts.isPresent()) { facts++; ev.add("Time to survive on the exposed site's own stock: " + n1(tts.getAsDouble()) + " days."); }
        }
        List<MaterialId> products = mat.map(ins::dependentProducts).orElse(List.of());
        ev.add(products.isEmpty() ? "The bill of materials lists no product that consumes this material." : "Products that consume it (bill of materials): " + products.stream().map(MaterialId::value).limit(6).toList() + ".");
        return Draft.advisory(ev, List.of("Requirement estimate assumes the exposed site runs at its nominal throughput."), List.of("Advisory: proposes no action itself."), facts / 5.0, focus);
    }
}
