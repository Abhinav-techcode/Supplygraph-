package com.supplygraph.domain.model.pareto;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Set of evaluated recovery solutions with the non-dominated Pareto frontier identified.
 */
public record ParetoFrontier(
        String evaluationId,
        String disruptionId,
        List<ParetoSolution> allSolutions,
        List<ParetoSolution> frontierSolutions,
        Instant evaluatedAt
) implements Serializable {

    public ParetoFrontier {
        Objects.requireNonNull(evaluationId, "evaluationId must not be null");
        Objects.requireNonNull(disruptionId, "disruptionId must not be null");
        allSolutions = allSolutions == null ? List.of() : List.copyOf(allSolutions);
        frontierSolutions = frontierSolutions == null ? List.of() : List.copyOf(frontierSolutions);
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
    }

    public static ParetoFrontier compute(String evaluationId, String disruptionId, List<ParetoSolution> candidates) {
        Objects.requireNonNull(evaluationId, "evaluationId must not be null");
        Objects.requireNonNull(disruptionId, "disruptionId must not be null");
        if (candidates == null || candidates.isEmpty()) {
            return new ParetoFrontier(evaluationId, disruptionId, List.of(), List.of(), Instant.now());
        }

        List<ParetoSolution> allMarked = new ArrayList<>();
        List<ParetoSolution> frontier = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            ParetoSolution current = candidates.get(i);
            boolean isDominated = false;

            for (int j = 0; j < candidates.size(); j++) {
                if (i != j && candidates.get(j).dominates(current)) {
                    isDominated = true;
                    break;
                }
            }

            ParetoSolution marked = ParetoSolution.of(
                    current.id(),
                    current.alternativeId(),
                    current.alternativeName(),
                    current.costUsd(),
                    current.recoveryTimeHours(),
                    current.unmetDemandUnits(),
                    current.serviceDegradationPct(),
                    current.supplierConcentrationRisk(),
                    !isDominated,
                    current.feasibilityScore(),
                    current.tradeOffExplanation()
            );

            allMarked.add(marked);
            if (!isDominated) {
                frontier.add(marked);
            }
        }

        return new ParetoFrontier(evaluationId, disruptionId, allMarked, frontier, Instant.now());
    }
}
