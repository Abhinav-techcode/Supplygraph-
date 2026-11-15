package com.supplygraph.concurguard.arbitration;

import com.supplygraph.concurguard.api.ArbitrationStatus;
import com.supplygraph.concurguard.conflict.Conflict;
import com.supplygraph.concurguard.model.IntentId;
import com.supplygraph.domain.model.capacity.CapacityWindow;
import com.supplygraph.domain.model.common.Quantity;
import com.supplygraph.domain.model.twin.StateVersion;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic arbitration decision record produced by ConcurGuard for an ActionIntent.
 */
public record ArbitrationDecision(
        DecisionId id,
        IntentId intentId,
        ArbitrationStatus outcome,
        List<Conflict> triggeringConflicts,
        String reasonCode,
        String explanation,
        Optional<CapacityWindow> suggestedWindow,
        Optional<Quantity> suggestedQuantity,
        StateVersion stateVersion,
        Instant arbitratedAt
) {
    public ArbitrationDecision {
        Objects.requireNonNull(id, "DecisionId must not be null");
        Objects.requireNonNull(intentId, "IntentId must not be null");
        Objects.requireNonNull(outcome, "Outcome must not be null");
        triggeringConflicts = triggeringConflicts == null ? List.of() : List.copyOf(triggeringConflicts);
        Objects.requireNonNull(reasonCode, "ReasonCode must not be null");
        Objects.requireNonNull(explanation, "Explanation must not be null");
        Objects.requireNonNull(suggestedWindow, "suggestedWindow optional must not be null");
        Objects.requireNonNull(suggestedQuantity, "suggestedQuantity optional must not be null");
        Objects.requireNonNull(stateVersion, "stateVersion must not be null");
        Objects.requireNonNull(arbitratedAt, "arbitratedAt must not be null");
    }

    public static ArbitrationDecision allow(IntentId intentId, StateVersion version) {
        return new ArbitrationDecision(
                DecisionId.generate(), intentId, ArbitrationStatus.ALLOW, List.of(),
                "APPROVED_NO_CONFLICT", "Plan proposal is sound and unconflicted",
                Optional.empty(), Optional.empty(), version, Instant.now()
        );
    }

    public static ArbitrationDecision reject(IntentId intentId, List<Conflict> conflicts, String reason, String explanation, StateVersion version) {
        return new ArbitrationDecision(
                DecisionId.generate(), intentId, ArbitrationStatus.REJECT, conflicts,
                reason, explanation, Optional.empty(), Optional.empty(), version, Instant.now()
        );
    }

    public static ArbitrationDecision requestReplanning(IntentId intentId, List<Conflict> conflicts, String reason, String explanation, StateVersion version) {
        return new ArbitrationDecision(
                DecisionId.generate(), intentId, ArbitrationStatus.REQUEST_REPLANNING, conflicts,
                reason, explanation, Optional.empty(), Optional.empty(), version, Instant.now()
        );
    }

    public static ArbitrationDecision reschedule(IntentId intentId, List<Conflict> conflicts, CapacityWindow newWindow, StateVersion version) {
        return new ArbitrationDecision(
                DecisionId.generate(), intentId, ArbitrationStatus.RESCHEDULE, conflicts,
                "RESCHEDULED_TIME_WINDOW", "Action rescheduled to non-colliding window",
                Optional.of(newWindow), Optional.empty(), version, Instant.now()
        );
    }
}
