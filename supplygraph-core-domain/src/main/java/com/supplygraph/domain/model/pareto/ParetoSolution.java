package com.supplygraph.domain.model.pareto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Evaluated recovery solution vector across multi-objective dimensions.
 */
public record ParetoSolution(
        ParetoSolutionId id,
        String alternativeId,
        String alternativeName,
        BigDecimal costUsd,
        double recoveryTimeHours,
        double unmetDemandUnits,
        double serviceDegradationPct,
        double supplierConcentrationRisk,
        boolean isDominant,
        double feasibilityScore,
        String tradeOffExplanation
) implements Serializable {

    public ParetoSolution {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(alternativeId, "alternativeId must not be null");
        Objects.requireNonNull(alternativeName, "alternativeName must not be null");
        costUsd = costUsd == null ? BigDecimal.ZERO : costUsd;
    }

    public static ParetoSolution of(
            ParetoSolutionId id,
            String alternativeId,
            String alternativeName,
            BigDecimal costUsd,
            double recoveryTimeHours,
            double unmetDemandUnits,
            double serviceDegradationPct,
            double supplierConcentrationRisk,
            boolean isDominant,
            double feasibilityScore,
            String tradeOffExplanation
    ) {
        return new ParetoSolution(
                id, alternativeId, alternativeName, costUsd,
                recoveryTimeHours, unmetDemandUnits, serviceDegradationPct,
                supplierConcentrationRisk, isDominant, feasibilityScore, tradeOffExplanation
        );
    }

    /**
     * Determines whether this solution Pareto-dominates another candidate solution across all 5 objectives.
     */
    public boolean dominates(ParetoSolution other) {
        if (other == null) {
            return true;
        }

        boolean atLeastAsGoodInAll =
                this.costUsd.compareTo(other.costUsd()) <= 0 &&
                this.recoveryTimeHours <= other.recoveryTimeHours() &&
                this.unmetDemandUnits <= other.unmetDemandUnits() &&
                this.serviceDegradationPct <= other.serviceDegradationPct() &&
                this.supplierConcentrationRisk <= other.supplierConcentrationRisk();

        boolean strictlyBetterInAtLeastOne =
                this.costUsd.compareTo(other.costUsd()) < 0 ||
                this.recoveryTimeHours < other.recoveryTimeHours() ||
                this.unmetDemandUnits < other.unmetDemandUnits() ||
                this.serviceDegradationPct < other.serviceDegradationPct() ||
                this.supplierConcentrationRisk < other.supplierConcentrationRisk();

        return atLeastAsGoodInAll && strictlyBetterInAtLeastOne;
    }
}
