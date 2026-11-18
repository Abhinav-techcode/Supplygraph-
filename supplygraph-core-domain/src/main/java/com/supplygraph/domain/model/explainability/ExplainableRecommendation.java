package com.supplygraph.domain.model.explainability;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Transparent, evidence-backed recovery recommendation with constraint evaluation,
 * trade-off quantification, and mandatory human sign-off requirement.
 */
public record ExplainableRecommendation(
        RecommendationId id,
        String disruptionId,
        String stateVersion,
        String title,
        String summary,
        List<String> primaryActions,
        BigDecimal expectedCostUsd,
        double expectedRecoveryTimeHours,
        double serviceLevelRestorationPct,
        List<DecisionEvidence> evidenceList,
        List<DecisionConstraint> constraintsEvaluated,
        List<DecisionTradeOff> tradeOffs,
        Map<String, String> uncertaintyFactors,
        boolean requiresHumanApproval,
        boolean approved,
        String approvedBy,
        Instant approvedAt,
        String rejectionReason
) implements Serializable {

    public ExplainableRecommendation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(disruptionId, "disruptionId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(summary, "summary must not be null");
        primaryActions = primaryActions == null ? List.of() : List.copyOf(primaryActions);
        expectedCostUsd = expectedCostUsd == null ? BigDecimal.ZERO : expectedCostUsd;
        evidenceList = evidenceList == null ? List.of() : List.copyOf(evidenceList);
        constraintsEvaluated = constraintsEvaluated == null ? List.of() : List.copyOf(constraintsEvaluated);
        tradeOffs = tradeOffs == null ? List.of() : List.copyOf(tradeOffs);
        uncertaintyFactors = uncertaintyFactors == null ? Map.of() : Map.copyOf(uncertaintyFactors);
    }

    public static ExplainableRecommendation createPending(
            RecommendationId id,
            String disruptionId,
            String stateVersion,
            String title,
            String summary,
            List<String> primaryActions,
            BigDecimal expectedCostUsd,
            double expectedRecoveryTimeHours,
            double serviceLevelRestorationPct,
            List<DecisionEvidence> evidenceList,
            List<DecisionConstraint> constraintsEvaluated,
            List<DecisionTradeOff> tradeOffs,
            Map<String, String> uncertaintyFactors
    ) {
        return new ExplainableRecommendation(
                id,
                disruptionId,
                stateVersion,
                title,
                summary,
                primaryActions,
                expectedCostUsd,
                expectedRecoveryTimeHours,
                serviceLevelRestorationPct,
                evidenceList,
                constraintsEvaluated,
                tradeOffs,
                uncertaintyFactors,
                true, // Always requires human approval
                false,
                null,
                null,
                null
        );
    }

    public ExplainableRecommendation withApproval(String authorizedUserId, Instant approvedAt) {
        Objects.requireNonNull(authorizedUserId, "Authorizing user must be specified");
        Objects.requireNonNull(approvedAt, "Approval timestamp must not be null");
        return new ExplainableRecommendation(
                id,
                disruptionId,
                stateVersion,
                title,
                summary,
                primaryActions,
                expectedCostUsd,
                expectedRecoveryTimeHours,
                serviceLevelRestorationPct,
                evidenceList,
                constraintsEvaluated,
                tradeOffs,
                uncertaintyFactors,
                requiresHumanApproval,
                true,
                authorizedUserId,
                approvedAt,
                null
        );
    }

    public ExplainableRecommendation withRejection(String rejectingUserId, String reason, Instant rejectedAt) {
        Objects.requireNonNull(rejectingUserId, "Rejecting user must be specified");
        Objects.requireNonNull(reason, "Rejection reason must be provided");
        return new ExplainableRecommendation(
                id,
                disruptionId,
                stateVersion,
                title,
                summary,
                primaryActions,
                expectedCostUsd,
                expectedRecoveryTimeHours,
                serviceLevelRestorationPct,
                evidenceList,
                constraintsEvaluated,
                tradeOffs,
                uncertaintyFactors,
                requiresHumanApproval,
                false,
                null,
                null,
                reason + " (by " + rejectingUserId + " at " + rejectedAt + ")"
        );
    }
}

