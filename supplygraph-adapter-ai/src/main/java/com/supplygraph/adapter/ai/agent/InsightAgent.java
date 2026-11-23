package com.supplygraph.adapter.ai.agent;

import com.supplygraph.application.agent.*;
import com.supplygraph.application.tool.ToolExecutionBoundary;
import com.supplygraph.application.tool.ToolExecutionRequest;
import com.supplygraph.application.tool.ToolExecutionResult;
import com.supplygraph.concurguard.model.*;
import com.supplygraph.concurguard.replanning.ReplanningScope;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.common.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
* Base for agents whose proposals are computed from the digital-twin snapshot ({@link TwinInsights}); they are rule-based Java analysis, not a language model.
 * The reported figure is data completeness: the share of the facts the agent needs that the twin actually contained. It is a measured ratio, not a probability.
 * An agent that cannot find the facts it needs proposes nothing and says so, rather than inventing an action.
 */
public abstract class InsightAgent extends BaseSpecializedAgent {

    /** What an agent concluded. {@code type == null} means advisory only (no action proposed). */
    protected record Draft(ActionType type, String target, String location, double quantity, String claimKind, String claimId, String claimFacility,
                           List<String> evidence, List<String> assumptions, List<String> limitations, double dataStrength, double risk, String focusNode) {
        static Draft advisory(List<String> evidence, List<String> assumptions, List<String> limitations, double dataStrength, String focusNode) {
            return new Draft(null, null, null, 0, "NONE", null, null, evidence, assumptions, limitations, dataStrength, 0.0, focusNode);
        }
    }

    private final Set<String> tools;

    protected InsightAgent(AgentRole role, Set<String> allowedTools) {
        super(role, allowedTools);
        this.tools = allowedTools;
    }

    /** @param rank 0 on the first investigation; n on the n-th replanning iteration (agents then prefer their next-best option). */
    protected abstract Draft analyse(TwinInsights ins, AgentInvestigationContext ctx, ToolExecutionBoundary boundary, int rank);

    @Override
    public AgentProposal investigate(AgentInvestigationContext ctx, ToolExecutionBoundary boundary) {
        return build(ctx, boundary, 0);
    }

    @Override
    public AgentProposal replan(AgentInvestigationContext ctx, ReplanningScope scope, ToolExecutionBoundary boundary) {
        return build(ctx, boundary, Math.max(1, scope.iteration()));
    }

    private AgentProposal build(AgentInvestigationContext ctx, ToolExecutionBoundary boundary, int rank) {
        TwinInsights ins = TwinInsights.of(ctx);
        Draft d = analyse(ins, ctx, boundary, rank);
        // The only number an agent reports about itself is a measured ratio: how many of the facts it needs were actually found in the twin.
        double confidence = clamp(d.dataStrength());
        List<String> evidence = new ArrayList<>(d.evidence());
        evidence.add("Data completeness: " + pct(confidence) + " of the facts this agent needs were found in the twin.");
        String incident = ctx.incident().incidentId();
        String tag = incident.substring(0, Math.min(8, incident.length()));
        String suffix = rank > 0 ? "-REV" + rank : "";
        List<ActionIntent> intents = new ArrayList<>();
        if (d.type() != null && d.target() != null) {
            Instant start = Instant.now().plusSeconds(rank * 12L * 3600);
            long hours = Math.max(24, ctx.incident().estimatedDowntimeHours());
            CapacityWindow window = CapacityWindow.of(start, start.plusSeconds(hours * 3600));
            UnitOfMeasure unit = ins.material().map(ins::unit).orElse(UnitOfMeasure.PIECE);
            Quantity qty = Quantity.of(BigDecimal.valueOf(Math.max(0, Math.round(d.quantity()))), unit);
            ActionIntent.Builder b = ActionIntent.builder()
                    .id(IntentId.of("INT-" + role().name() + "-" + tag + suffix))
                    .incidentId(incident)
                    .agentId(role().agentId())
                    .actionType(d.type())
                    .targetEntityId(d.target())
                    .expectedStateVersion(ins.version())
                    .timeWindow(window)
                    .riskScore(clamp(d.risk()));
            if (d.location() != null) b.location(d.location());
            if (qty.isPositive()) {
                b.quantity(qty);
                if ("INVENTORY".equals(d.claimKind())) {
                    b.addResourceClaim(ResourceClaim.consumptive(ResourceIdentity.of("INVENTORY_ACCOUNT", d.claimId(), d.claimFacility()), qty, window));
                } else if ("LANE".equals(d.claimKind())) {
                    b.addResourceClaim(ResourceClaim.shared(ResourceIdentity.of("TRANSPORT_LANE", d.claimId(), d.claimFacility()), qty, window));
                }
            }
            intents.add(b.build());
        }
        return new AgentProposal("PROP-" + role().name() + "-" + tag + suffix, role(), role().agentId(), intents, evidence,
                d.assumptions(), confidence, d.limitations(), Instant.now());
    }

    /** Calls a read-only tool inside the zero-trust boundary; returns null when the tool refuses or fails. */
    protected ToolExecutionResult tool(ToolExecutionBoundary boundary, AgentInvestigationContext ctx, String name, Map<String, Object> params) {
        if (!tools.contains(name)) return null;
        try {
            ToolExecutionResult r = boundary.execute(new ToolExecutionRequest(name, identity(), ctx.incident().workspaceId(), params));
            return r.success() ? r : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    protected static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }
    protected static String pct(double v) { return Math.round(v * 100) + "%"; }
    protected static String n0(double v) { return String.format("%,.0f", v); }
    protected static String n1(double v) { return String.format("%.1f", v); }
}
