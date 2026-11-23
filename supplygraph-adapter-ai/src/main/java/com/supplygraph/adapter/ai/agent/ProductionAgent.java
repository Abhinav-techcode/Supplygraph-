package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.*;

import java.util.*;

/** Re-sequences production at the exposed site to protect output while stock lasts. */
public class ProductionAgent extends InsightAgent {
    public ProductionAgent() { super(AgentRole.PRODUCTION, Set.of("query_capacity", "query_bom")); }

    @Override
    protected Draft analyse(TwinInsights ins, AgentInvestigationContext ctx, ToolExecutionBoundary b, int rank) {
        List<String> ev = new ArrayList<>();
        Optional<NodeId> site = ins.exposedSite();
        Optional<MaterialId> mat = ins.material();
        if (site.isEmpty()) return Draft.advisory(List.of("No downstream production site is exposed."), List.of(), List.of(), 0.0, "GLOBAL");
        String s = site.get().value();
        double perHour = ins.throughputPerHour(site.get());
        ToolExecutionResult cap = tool(b, ctx, "query_capacity", Map.of("nodeId", s));
        double facts = 1 + (perHour > 0 ? 1 : 0) + (mat.isPresent() ? 1 : 0);
        ev.add(s + " is the most exposed production site" + (perHour > 0 ? " (nominal throughput " + n0(perHour) + " units/h)." : " (throughput unknown in the twin)."));
        double onHandAtSite = mat.map(m -> ins.onHand(site.get(), m)).orElse(0.0);
        OptionalDouble tts = mat.isPresent() ? ins.timeToSurviveDays(mat.get()) : OptionalDouble.empty();
        double downtimeDays = Math.max(1.0, ctx.incident().estimatedDowntimeHours() / 24.0);
        double risk = 0.3;
        if (tts.isPresent()) { facts++; ev.add("Own stock lasts " + n1(tts.getAsDouble()) + " days against a " + n1(downtimeDays) + "-day outage."); risk = clamp(1 - tts.getAsDouble() / downtimeDays); }
        ev.add(cap != null ? "Capacity calendar tool answered for " + s + "." : "Capacity calendar not available for " + s + ".");
        double dayOutput = perHour * 24;
        double qty = Math.min(dayOutput, onHandAtSite > 0 ? onHandAtSite : dayOutput);
        return new Draft(ActionType.SPLIT_PRODUCTION_BATCH, s, s, qty, onHandAtSite > 0 && mat.isPresent() ? "INVENTORY" : "NONE", mat.map(MaterialId::value).orElse(null), s, ev,
                List.of("Variants that do not use the short material can be sequenced first."), List.of("Variant mix is not modelled; the quantity is one day of nominal output."),
                facts / 4.0, risk, s);
    }
}
