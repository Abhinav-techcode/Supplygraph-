package com.supplygraph.concurguard.replanning;

import com.supplygraph.concurguard.arbitration.ArbitrationDecision;
import com.supplygraph.concurguard.model.ActionIntent;
import com.supplygraph.concurguard.model.IntentId;

import java.util.*;

/**
 * Final coordinated outcome of selective replanning.
 */
public record ReplanningResult(
        boolean success,
        List<ActionIntent> finalPlan,
        Map<IntentId, ArbitrationDecision> finalDecisions,
        int iterationsCompleted,
        Set<IntentId> preservedIntents,
        Optional<String> escalationReason
) {
    public ReplanningResult {
        finalPlan = finalPlan == null ? List.of() : List.copyOf(finalPlan);
        finalDecisions = finalDecisions == null ? Map.of() : Collections.unmodifiableMap(Map.copyOf(finalDecisions));
        preservedIntents = preservedIntents == null ? Set.of() : Set.copyOf(preservedIntents);
        Objects.requireNonNull(escalationReason, "escalationReason optional must not be null");
    }

    public static ReplanningResult successful(List<ActionIntent> plan, Map<IntentId, ArbitrationDecision> decisions,
                                              int iterations, Set<IntentId> preserved) {
        return new ReplanningResult(true, plan, decisions, iterations, preserved, Optional.empty());
    }

    public static ReplanningResult escalated(List<ActionIntent> plan, Map<IntentId, ArbitrationDecision> decisions,
                                             int iterations, Set<IntentId> preserved, String reason) {
        return new ReplanningResult(false, plan, decisions, iterations, preserved, Optional.of(reason));
    }
}
