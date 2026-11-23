package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.concurguard.model.ActionType;
import com.supplygraph.domain.model.bom.MaterialId;
import com.supplygraph.domain.model.graph.*;

import java.util.*;

/** Puts stock held at a disrupted manufacturing site on hold pending inspection (e.g. contamination or flooding), otherwise advises only. */
public class QualityAgent extends InsightAgent {
    public QualityAgent() { super(AgentRole.QUALITY, Set.of("query_inventory", "query_bom")); }

    @Override
    protected Draft analyse(TwinInsights ins, AgentInvestigationContext ctx, ToolExecutionBoundary b, int rank) {
        List<String> ev = new ArrayList<>();
        Optional<MaterialId> mat = ins.material();
        String focus = ins.hitNodes().stream().map(NodeId::value).findFirst().orElse("GLOBAL");
        if (!ins.hasAffectedNodes() || mat.isEmpty()) return Draft.advisory(List.of("No disrupted facility or material identified; no quality hold evaluated."), List.of(), List.of(), 0.0, focus);
        List<NodeId> sites = ins.hitNodes().stream().filter(n -> ins.graph().getNode(n).getType() == NodeType.MANUFACTURING_SITE).sorted(Comparator.comparing(NodeId::value)).toList();
        for (NodeId n : sites) {
            double q = ins.onHand(n, mat.get());
            if (q > 0) {
                ToolExecutionResult inv = tool(b, ctx, "query_inventory", Map.of("materialId", mat.get().value(), "nodeId", n.value()));
                ev.add(n.value() + " is a disrupted manufacturing site holding " + n0(q) + " units of " + ins.materialName(mat.get()) + (inv != null ? " (confirmed by inventory tool)." : "."));
                ev.add("Stock exposed to the incident must be inspected before it can be consumed or shipped.");
                return new Draft(ActionType.QUARANTINE_LOT, n.value(), n.value(), q, "INVENTORY", mat.get().value(), n.value(), ev,
                        List.of("The disruption (" + ctx.incident().title() + ") may have affected the condition of stock on site."), List.of("Hold is reversible after inspection."), 0.85, 0.1, n.value());
            }
        }
        ev.add(sites.isEmpty() ? "Disrupted facilities are not manufacturing sites; no inspection hold is required." : "Disrupted manufacturing sites hold no stock of " + ins.materialName(mat.get()) + "; nothing to quarantine.");
        return Draft.advisory(ev, List.of(), List.of("Advisory: proposes no action."), 0.85, focus);
    }
}
