package com.supplygraph.domain.model.sustainability;

import com.supplygraph.domain.model.WorkspaceId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Decarbonization pathway scenario evaluating emissions reduction against marginal abatement cost.
 */
public record EnvironmentalScenario(
        EnvironmentalScenarioId id,
        WorkspaceId workspaceId,
        String scenarioName,
        double baselineCo2Tons,
        double projectedCo2Tons,
        double reductionPercentage,
        BigDecimal totalAbatementCostUsd,
        BigDecimal costPerTonCo2AvoidedUsd,
        Instant evaluatedAt
) implements Serializable {
    public EnvironmentalScenario {
        Objects.requireNonNull(id, "Scenario ID cannot be null");
        Objects.requireNonNull(workspaceId, "Workspace ID cannot be null");
        Objects.requireNonNull(scenarioName, "Scenario name cannot be null");
        Objects.requireNonNull(totalAbatementCostUsd, "Abatement cost cannot be null");
        Objects.requireNonNull(costPerTonCo2AvoidedUsd, "Cost per ton cannot be null");
        Objects.requireNonNull(evaluatedAt, "Evaluated timestamp cannot be null");
    }
}

