package com.supplygraph.domain.model.recovery;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * High-level candidate recovery plan alternative evaluated across multi-objective dimensions.
 */
public record RecoveryPlanAlternative(
        PlanAlternativeId id,
        String name,
        String description,
        BigDecimal expectedCost,
        double timeToRecoverHours,
        double serviceLevelRestoration,
        List<String> actions
) implements Serializable {

    public RecoveryPlanAlternative {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        expectedCost = expectedCost == null ? BigDecimal.ZERO : expectedCost;
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public static RecoveryPlanAlternative of(
            PlanAlternativeId id,
            String name,
            String description,
            BigDecimal expectedCost,
            double timeToRecoverHours,
            double serviceLevelRestoration,
            List<String> actions
    ) {
        return new RecoveryPlanAlternative(id, name, description, expectedCost, timeToRecoverHours, serviceLevelRestoration, actions);
    }
}

