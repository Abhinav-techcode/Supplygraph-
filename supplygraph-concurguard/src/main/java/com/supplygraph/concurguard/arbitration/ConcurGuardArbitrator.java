package com.supplygraph.concurguard.arbitration;

import com.supplygraph.concurguard.api.ArbitrationStatus;
import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.conflict.Conflict;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.domain.model.twin.StateVersion;

import java.util.*;

/**
 * Deterministic arbitration engine enforcing documented precedence:
 * Safety/Regulatory Policies > Graph Cycles > Stale State > Quantitative Scarcity.
 */
public class ConcurGuardArbitrator {

    public Map<IntentId, ArbitrationDecision> arbitrate(
            Collection<ActionIntent> intents,
            List<Conflict> conflicts,
            StateVersion currentVersion
    ) {
        Map<IntentId, ArbitrationDecision> decisions = new LinkedHashMap<>();
        Map<IntentId, List<Conflict>> conflictsByIntent = new HashMap<>();

        for (Conflict c : conflicts) {
            conflictsByIntent.computeIfAbsent(c.primaryIntentId(), k -> new ArrayList<>()).add(c);
            c.conflictingIntentId().ifPresent(confId ->
                    conflictsByIntent.computeIfAbsent(confId, k -> new ArrayList<>()).add(c));
        }

        Map<IntentId, ActionIntent> intentMap = new LinkedHashMap<>();
        for (ActionIntent i : intents) {
            intentMap.put(i.id(), i);
        }

        // Process intents with conflicts according to strict precedence
        for (ActionIntent intent : intents) {
            List<Conflict> intentConflicts = conflictsByIntent.getOrDefault(intent.id(), List.of());

            if (intentConflicts.isEmpty()) {
                decisions.put(intent.id(), ArbitrationDecision.allow(intent.id(), currentVersion));
                continue;
            }

            // Precedence 1: Policy Violation -> REJECT
            Optional<Conflict> policyConflict = intentConflicts.stream()
                    .filter(c -> c.type() == ConflictType.INDUSTRY_POLICY_VIOLATION)
                    .findFirst();
            if (policyConflict.isPresent()) {
                decisions.put(intent.id(), ArbitrationDecision.reject(
                        intent.id(), intentConflicts, "POLICY_VIOLATION",
                        policyConflict.get().explanation(), currentVersion));
                continue;
            }

            // Precedence 2: Dependency Deadlock / Cycle -> REQUEST_REPLANNING
            Optional<Conflict> depConflict = intentConflicts.stream()
                    .filter(c -> c.type() == ConflictType.DEPENDENCY_CONFLICT)
                    .findFirst();
            if (depConflict.isPresent()) {
                decisions.put(intent.id(), ArbitrationDecision.requestReplanning(
                        intent.id(), intentConflicts, "DEPENDENCY_CYCLE_OR_MISSING",
                        depConflict.get().explanation(), currentVersion));
                continue;
            }

            // Precedence 3: Stale State -> REQUEST_REPLANNING
            Optional<Conflict> staleConflict = intentConflicts.stream()
                    .filter(c -> c.type() == ConflictType.STALE_STATE_CONFLICT)
                    .findFirst();
            if (staleConflict.isPresent()) {
                decisions.put(intent.id(), ArbitrationDecision.requestReplanning(
                        intent.id(), intentConflicts, "STALE_DIGITAL_TWIN_VERSION",
                        staleConflict.get().explanation(), currentVersion));
                continue;
            }

            // Precedence 4: Semantic Alias Collision -> REQUEST_REPLANNING
            Optional<Conflict> aliasConflict = intentConflicts.stream()
                    .filter(c -> c.type() == ConflictType.SEMANTIC_ALIAS)
                    .findFirst();
            if (aliasConflict.isPresent()) {
                decisions.put(intent.id(), ArbitrationDecision.requestReplanning(
                        intent.id(), intentConflicts, "SEMANTIC_ALIAS_AMBIGUITY",
                        aliasConflict.get().explanation(), currentVersion));
                continue;
            }

            // Precedence 5: Scarcity Contention (Quantity or Capacity)
            // Compare competing proposals: Favor lower risk and lower estimated cost
            Conflict compConflict = intentConflicts.get(0);
            IntentId rivalId = compConflict.primaryIntentId().equals(intent.id())
                    ? compConflict.conflictingIntentId().orElse(null)
                    : compConflict.primaryIntentId();

            if (rivalId != null) {
                ActionIntent rival = intentMap.get(rivalId);

                if (rival != null) {
                    if (intent.riskScore() < rival.riskScore() ||
                            (intent.riskScore() == rival.riskScore() && intent.estimatedCost().compareTo(rival.estimatedCost()) < 0) ||
                            (intent.riskScore() == rival.riskScore() && intent.estimatedCost().compareTo(rival.estimatedCost()) == 0 && intent.id().value().compareTo(rival.id().value()) < 0)) {
                        // This intent wins contention
                        decisions.put(intent.id(), ArbitrationDecision.allow(intent.id(), currentVersion));
                        continue;
                    } else {
                        // Rival won; this intent must replan
                        decisions.put(intent.id(), ArbitrationDecision.requestReplanning(
                                intent.id(), intentConflicts, "RESOURCE_CONTENTION_LOST",
                                "Out-competed by lower risk/cost proposal " + rival.id(), currentVersion));
                        continue;
                    }
                }
            }

            // Default fallback for remaining unresolvable contention
            decisions.put(intent.id(), ArbitrationDecision.requestReplanning(
                    intent.id(), intentConflicts, "UNRESOLVED_CONTENTION",
                    compConflict.explanation(), currentVersion));
        }

        return Collections.unmodifiableMap(decisions);
    }
}
