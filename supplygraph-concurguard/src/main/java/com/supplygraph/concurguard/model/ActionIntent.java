package com.supplygraph.concurguard.model;

import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.twin.StateVersion;

import java.math.BigDecimal;
import java.util.*;

/**
 * Immutable ActionIntent representing an agent's proposed intervention.
 * Undergoes strict structural and semantic validation before entering ConcurGuard.
 */
public record ActionIntent(
        IntentId id,
        String incidentId,
        AgentId agentId,
        ActionType actionType,
        String targetEntityId,
        List<ResourceClaim> resourceClaims,
        Quantity quantity,
        CapacityClaim capacityClaim,
        CapacityWindow timeWindow,
        String location,
        Set<IntentId> dependencies,
        List<String> preconditions,
        StateVersion expectedStateVersion,
        List<String> evidenceReferences,
        List<String> assumptions,
        BigDecimal estimatedCost,
        double riskScore,
        boolean reversible,
        boolean requiresHumanApproval
) {
    public ActionIntent {
        Objects.requireNonNull(id, "IntentId must not be null");
        Objects.requireNonNull(incidentId, "IncidentId must not be null");
        Objects.requireNonNull(agentId, "AgentId must not be null");
        Objects.requireNonNull(actionType, "ActionType must not be null");
        Objects.requireNonNull(targetEntityId, "TargetEntityId must not be null");
        resourceClaims = resourceClaims == null ? List.of() : List.copyOf(resourceClaims);
        dependencies = dependencies == null ? Set.of() : Set.copyOf(dependencies);
        preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
        expectedStateVersion = expectedStateVersion == null ? StateVersion.initial() : expectedStateVersion;
        evidenceReferences = evidenceReferences == null ? List.of() : List.copyOf(evidenceReferences);
        assumptions = assumptions == null ? List.of() : List.copyOf(assumptions);
        if (estimatedCost == null) {
            estimatedCost = BigDecimal.ZERO;
        }
        if (riskScore < 0.0 || riskScore > 1.0) {
            throw new IllegalArgumentException("Risk score must be in range [0.0, 1.0]: " + riskScore);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private IntentId id;
        private String incidentId = "INC-DEFAULT";
        private AgentId agentId;
        private ActionType actionType;
        private String targetEntityId;
        private List<ResourceClaim> resourceClaims = new ArrayList<>();
        private Quantity quantity;
        private CapacityClaim capacityClaim;
        private CapacityWindow timeWindow;
        private String location = "GLOBAL";
        private Set<IntentId> dependencies = new LinkedHashSet<>();
        private List<String> preconditions = new ArrayList<>();
        private StateVersion expectedStateVersion = StateVersion.initial();
        private List<String> evidenceReferences = new ArrayList<>();
        private List<String> assumptions = new ArrayList<>();
        private BigDecimal estimatedCost = BigDecimal.ZERO;
        private double riskScore = 0.1;
        private boolean reversible = true;
        private boolean requiresHumanApproval = false;

        public Builder id(IntentId id) { this.id = id; return this; }
        public Builder id(String id) { this.id = IntentId.of(id); return this; }
        public Builder incidentId(String incidentId) { this.incidentId = incidentId; return this; }
        public Builder agentId(AgentId agentId) { this.agentId = agentId; return this; }
        public Builder actionType(ActionType actionType) { this.actionType = actionType; return this; }
        public Builder targetEntityId(String targetEntityId) { this.targetEntityId = targetEntityId; return this; }
        public Builder addResourceClaim(ResourceClaim claim) { this.resourceClaims.add(claim); return this; }
        public Builder resourceClaims(List<ResourceClaim> claims) { this.resourceClaims = new ArrayList<>(claims); return this; }
        public Builder quantity(Quantity quantity) { this.quantity = quantity; return this; }
        public Builder capacityClaim(CapacityClaim capacityClaim) { this.capacityClaim = capacityClaim; return this; }
        public Builder timeWindow(CapacityWindow timeWindow) { this.timeWindow = timeWindow; return this; }
        public Builder location(String location) { this.location = location; return this; }
        public Builder addDependency(IntentId dependency) { this.dependencies.add(dependency); return this; }
        public Builder dependencies(Set<IntentId> dependencies) { this.dependencies = new LinkedHashSet<>(dependencies); return this; }
        public Builder addPrecondition(String precondition) { this.preconditions.add(precondition); return this; }
        public Builder expectedStateVersion(StateVersion version) { this.expectedStateVersion = version; return this; }
        public Builder addEvidence(String evidence) { this.evidenceReferences.add(evidence); return this; }
        public Builder addAssumption(String assumption) { this.assumptions.add(assumption); return this; }
        public Builder estimatedCost(BigDecimal cost) { this.estimatedCost = cost; return this; }
        public Builder riskScore(double riskScore) { this.riskScore = riskScore; return this; }
        public Builder reversible(boolean reversible) { this.reversible = reversible; return this; }
        public Builder requiresHumanApproval(boolean requiresHumanApproval) { this.requiresHumanApproval = requiresHumanApproval; return this; }

        public ActionIntent build() {
            if (id == null) {
                id = IntentId.generate();
            }
            Objects.requireNonNull(agentId, "AgentId must be set");
            Objects.requireNonNull(actionType, "ActionType must be set");
            Objects.requireNonNull(targetEntityId, "TargetEntityId must be set");
            return new ActionIntent(
                    id, incidentId, agentId, actionType, targetEntityId, resourceClaims,
                    quantity, capacityClaim, timeWindow, location, dependencies, preconditions,
                    expectedStateVersion, evidenceReferences, assumptions, estimatedCost,
                    riskScore, reversible, requiresHumanApproval
            );
        }
    }
}
